/**
 * Licensed to WibiData, Inc. under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  WibiData, Inc.
 * licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.kiji.avro.mapreduce;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * Writes Avro records to an Avro container file output stream.
 *
 * @param <T> The Java type of the Avro data to write.
 */
public class AvroKeyRecordWriter<T> extends RecordWriter<AvroKey<T>, NullWritable> {
  /** A writer for the Avro container file. */
  private final DataFileWriter<T> mAvroFileWriter;

  /**
   * Constructor.
   *
   * @param writerSchema The writer schema for the records in the Avro container file.
   * @param compressionCodec A compression codec factory for the Avro container file.
   * @param outputStream The output stream to write the Avro container file to.
   * @throws IOException If the record writer cannot be opened.
   */
  public AvroKeyRecordWriter(Schema writerSchema, CodecFactory compressionCodec,
      OutputStream outputStream) throws IOException {
    // Create an Avro container file and a writer to it.
    mAvroFileWriter = new DataFileWriter<T>(new SpecificDatumWriter<T>(writerSchema));
    mAvroFileWriter.setCodec(compressionCodec);
    mAvroFileWriter.create(writerSchema, outputStream);
  }

  /** {@inheritDoc} */
  @Override
  public void write(AvroKey<T> record, NullWritable ignore) throws IOException {
    mAvroFileWriter.append(record.datum());
  }

  /** {@inheritDoc} */
  @Override
  public void close(TaskAttemptContext context) throws IOException {
    mAvroFileWriter.close();
  }
}