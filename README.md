kiji-avro
=========

Extensions to the [Apache Avro](http://avro.apache.org) project for
working with [Apache Hadoop](http://hadoop.apache.org).  Many of the
components in this project have been moved into the Avro project,
though this repository serves as a good home for the application-level
features not part of the Avro core.


### Features

* `org.apache.hadoop.io.AvroSequenceFile` - A wrapper around a Hadoop
  SequenceFile that supports reading and writing Avro data.
* `org.apache.avro.mapreduce` - A package for using Avro as
  input/output records from a Hadoop MapReduce job using the new API.


### How to build

To build `target/kiji-avro-${project.version}.jar` run:

    mvn package


### Avro versions supported

For avro-1.6.x and earlier, use odiago-avro 1.0.6. (This project used to be
called odiago-avro.) As of avro-1.7.0, the kiji-avro project has been included
in avro-mapred-1.7.0 as part of Apache Avro's release.

The Apache Avro project is currently only configured to release
avro-mapred-1.7.x compiled against CDH3 (Hadoop 0.20). To additionally
support CDH4 (Hadoop 2.0), the kiji-avro project maintains only the
code that is affected by the changes in Hadoop 2.0. Therefore, there
are two branches of kiji-avro code and releases as of v1.0.7:

* master: kiji-avro-1.0.8-cdh4
* cdh3: odiago-avro-1.0.7-cdh3
