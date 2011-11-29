// (c) Copyright 2011 Odiago, Inc.

package org.apache.avro.mapreduce;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.AvroKeyValue;
import org.apache.avro.io.DatumReader;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.AvroValue;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that Avro container files of generic records with two fields 'key' and 'value'
 * can be read by the AvroKeyValueInputFormat.
 */
public class TestKeyValueInput {
  @Rule
  public TemporaryFolder mTempDir = new TemporaryFolder();

  /**
   * Creates an Avro file of <docid, text> pairs to use for test input:
   *
   * +-----+-----------------------+
   * | KEY | VALUE                 |
   * +-----+-----------------------+
   * | 1   | "apple banana carrot" |
   * | 2   | "apple banana"        |
   * | 3   | "apple"               |
   * +-----+-----------------------+
   *
   * @return The avro file.
   */
  private File createInputFile() throws IOException {
    Schema keyValueSchema = AvroKeyValue.getSchema(
        Schema.create(Schema.Type.INT), Schema.create(Schema.Type.STRING));

    AvroKeyValue<Integer, CharSequence> record1
        = new AvroKeyValue<Integer, CharSequence>(new GenericData.Record(keyValueSchema));
    record1.setKey(1);
    record1.setValue("apple banana carrot");

    AvroKeyValue<Integer, CharSequence> record2
        = new AvroKeyValue<Integer, CharSequence>(new GenericData.Record(keyValueSchema));
    record2.setKey(2);
    record2.setValue("apple banana");

    AvroKeyValue<Integer, CharSequence> record3
        = new AvroKeyValue<Integer, CharSequence>(new GenericData.Record(keyValueSchema));
    record3.setKey(3);
    record3.setValue("apple");

    return AvroFiles.createFile(new File(mTempDir.getRoot(), "inputKeyValues.avro"),
        keyValueSchema, record1.get(), record2.get(), record3.get());
  }

  /** A mapper for indexing documents. */
  public static class IndexMapper
      extends Mapper<AvroKey<Integer>, AvroValue<CharSequence>, Text, IntWritable> {
    @Override
    protected void map(AvroKey<Integer> docid, AvroValue<CharSequence> body, Context context)
        throws IOException, InterruptedException {
      for (String token : body.datum().toString().split(" ")) {
        context.write(new Text(token), new IntWritable(docid.datum()));
      }
    }
  }

  /** A reducer for aggregating token to docid mapping into a hitlist. */
  public static class IndexReducer
      extends Reducer<Text, IntWritable, Text, AvroValue<List<Integer>>> {
    @Override
    protected void reduce(Text token, Iterable<IntWritable> docids, Context context)
        throws IOException, InterruptedException {
      List<Integer> hitlist = new ArrayList<Integer>();
      for (IntWritable docid : docids) {
        hitlist.add(docid.get());
      }
      context.write(token, new AvroValue<List<Integer>>(hitlist));
    }
  }

  @Test
  public void testKeyValueInput()
      throws ClassNotFoundException, IOException, InterruptedException {
    // Create a test input file.
    File inputFile = createInputFile();

    // Configure the job input.
    Job job = new Job();
    FileInputFormat.setInputPaths(job, new Path(inputFile.getAbsolutePath()));
    job.setInputFormatClass(AvroKeyValueInputFormat.class);
    AvroJob.setInputKeySchema(job, Schema.create(Schema.Type.INT));
    AvroJob.setInputValueSchema(job, Schema.create(Schema.Type.STRING));

    // Configure a mapper.
    job.setMapperClass(IndexMapper.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(IntWritable.class);

    // Configure a reducer.
    job.setReducerClass(IndexReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(AvroValue.class);
    AvroJob.setOutputValueSchema(job, Schema.createArray(Schema.create(Schema.Type.INT)));

    // Configure the output format.
    job.setOutputFormatClass(AvroKeyValueOutputFormat.class);
    Path outputPath = new Path(mTempDir.getRoot().getPath(), "out-index");
    FileOutputFormat.setOutputPath(job, outputPath);

    // Run the job.
    assertTrue(job.waitForCompletion(true));

    // Verify that the output Avro container file as the expected data.
    File avroFile = new File(outputPath.toString(), "part-r-00000.avro");
    DatumReader<GenericRecord> datumReader = new SpecificDatumReader<GenericRecord>(
        AvroKeyValue.getSchema(Schema.create(Schema.Type.STRING),
            Schema.createArray(Schema.create(Schema.Type.INT))));
    DataFileReader<GenericRecord> avroFileReader
        = new DataFileReader<GenericRecord>(avroFile, datumReader);
    assertTrue(avroFileReader.hasNext());

    AvroKeyValue<CharSequence, List<Integer>> appleRecord
        = new AvroKeyValue<CharSequence, List<Integer>>(avroFileReader.next());
    assertNotNull(appleRecord.get());
    assertEquals("apple", appleRecord.getKey().toString());
    List<Integer> appleDocs = appleRecord.getValue();
    assertEquals(3, appleDocs.size());
    assertTrue(appleDocs.contains(1));
    assertTrue(appleDocs.contains(2));
    assertTrue(appleDocs.contains(3));

    assertTrue(avroFileReader.hasNext());
    AvroKeyValue<CharSequence, List<Integer>> bananaRecord
        = new AvroKeyValue<CharSequence, List<Integer>>(avroFileReader.next());
    assertNotNull(bananaRecord.get());
    assertEquals("banana", bananaRecord.getKey().toString());
    List<Integer> bananaDocs = bananaRecord.getValue();
    assertEquals(2, bananaDocs.size());
    assertTrue(bananaDocs.contains(1));
    assertTrue(bananaDocs.contains(2));

    assertTrue(avroFileReader.hasNext());
    AvroKeyValue<CharSequence, List<Integer>> carrotRecord
        = new AvroKeyValue<CharSequence, List<Integer>>(avroFileReader.next());
    assertEquals("carrot", carrotRecord.getKey().toString());
    List<Integer> carrotDocs = carrotRecord.getValue();
    assertEquals(1, carrotDocs.size());
    assertTrue(carrotDocs.contains(1));

    assertFalse(avroFileReader.hasNext());
    avroFileReader.close();
  }
}
