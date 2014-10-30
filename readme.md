![LDBC Logo](ldbc_logo.png)
Semantic Publishing Benchmark
-----------------------------

###Introduction

Semantic Publishing Benchmark is an LDBC benchmark for testing the performance of RDF databases inspired by the Media/Publishing industry.
Performance is measured by producing a workload of CRUD (Create, Read, Update, Delete) operations which are executed simultaneously.

The benchmark offers a data generator that uses real reference data to produce synthetic datasets of various sizes and tests the scalability aspect
of RDF systems. The benchmark workload consists of editorial operations that add new, update or delete existing data and aggregation
operations that retrieve content according to various criteria. The aggregation queries define different "choke points", that is technical 
challenges that a standard RDF database must overcome thus giving opportunities for improvement regarding query optimization.

The benchmark also tests conformance for various rules inside the OWL2-RL rule-set.

###Build

Apache Ant build tool is required. Start one of the following tasks : 

```
#to build a standard version of the benchmark, compliant to SPARQL 1.1 standard
$ ant build-basic-querymix

#to build a standard version of the benchmark, compliant to SPARQL 1.1 standard with extended query mix
$ ant build-advanced-querymix

#to build a version of the benchmark customized for Virtuoso Database
$ ant build-basic-querymix-virtuoso

#to build a version of the benchmark customized for Virtuoso Database with extended query mix
$ ant build-advanced-querymix-virtuoso
```

Result of the build process is saved to the distribution folder (dist/) : 
* semantic_publishing_benchmark-*.jar - the benchmark driver
* data/ - folder containing all necessary data to run the benchmark
* test.properties - a configuration file with parameters for configuring the benchmark driver
* definitions.properties - a configuration file with pre-allocated values used by the benchmark. Not to be modified by the regular benchmark user.
* readme.txt

###Install

All necessary files required to run the benchmark are saved to the 'dist/' folder. The benchmark can be started from there or can be moved to a new location.
Optionally, additinal reference datasets can be added - they can be dowloaded from https://github.com/ldbc/ldbc_spb_optional_datasets. All files should be unzipped in folder 'data/datasets/'

###Configure

* RDF Repository configuration
  * Use RDFS rule-set
  * Enable context indexing (if available)
  * Enable text indexing (if available)
  * Enable geo-spatial indexing (if available)

* Actions like data generation or running the benchmark consist of executing a sequence of operational phases described here: https://github.com/ldbc/ldbc_spb_bm/wiki/Operational-Phases
  List of actions that the benchmark can perform: https://github.com/ldbc/ldbc_spb_bm/wiki/Benchmark-Actions
 
###Run

```sh
java -jar semantic_publishing_benchmark-*.jar test.properties
```
*Note: appropriate value for java maximum heap size may be required, e.g. -Xmx8G*

###Results
Logging details can be controlled by a configuration file: log4j.xml saved in the distributed benchmark driver (semantic_publishing_benchmark.jar). After modifying log4j.xml, benchmark driver must be updated with contents of the new xml file.
Results of the benchmark are saved to three types of log files :

* ***brief*** - brief log of executed queries, saved in semantic_publishing_benchmark_queries_brief.log
* ***detailed*** - detailed log of executed queries with results, saved in semantic_publishing_benchmark_queries_detailed.log
* ***summary*** - editorial and aggregate operations rate, saved in semantic_publishing_benchmark_results.log