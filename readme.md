![LDBC Logo](ldbc_logo.png)
Semantic Publishing Benchmark
-----------------------------

###Introduction

The Semantic Publishing Benchmark is an LDBC benchmark which measures the performance of RDF databases. Creation of that benchmark has been inspired 
by the Media/Publishing industry where requirements like: constant update of content or various requests for data extraction and aggregation are present.

Features of the benchmark:
* Provides a Data Generator using real reference datasets and producing synthetic data of various scales. 
* Workload consists of editorial operations (adding new, updating or deleting existing data) and aggregation operations (retrieve content, aggregate results, etc.). 
Aggregation operations consist of a wide range of queries, e.g. _search, aggregation, FTS, Faceted Search, Geo-spatial, Drill-down_ which define various types of _choke points_ (i.e. technical challenges) that an RDF database must successfully overcome.
* Provides validation of results
* Tests the conformance of the RDF Database to various rules inside the OWL2-RL rule-set.

###Build

Apache Ant build tool is required.

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

Result of the build process is saved to a distribution folder: _'dist/'_ : 
* ***semantic_publishing_benchmark.jar*** - the benchmark test driver
* ***data/*** - folder containing all necessary data to run the benchmark
* ***test.properties*** - a configuration file with parameters for configuring the benchmark driver
* ***definitions.properties*** - a configuration file with pre-allocated values used by the benchmark. Not to be modified by the regular benchmark user.
* ***readme.txt***

###Install

All necessary files required to run the benchmark are saved to folder: 'dist/'. The benchmark can be started from there or can be moved to a new location.
Optionally, additinal reference datasets can be added - they can be dowloaded from https://github.com/ldbc/ldbc_spb_optional_datasets. All files should be unzipped in folder 'data/datasets/'

###Configure

* ***RDF Repository configuration***
  * Use RDFS rule-set
  * Enable context indexing (if available)
  * Enable text indexing (if available)
  * Enable geo-spatial indexing (if available)

* ***Actions*** are the essential tasks that the benchmark driver can perform e.g. Data generation, Validation or Measure performance.
List of actions: https://github.com/ldbc/ldbc_spb_bm/wiki/Benchmark-Actions Each action consists of a set of operational phases that are executed sequentially. 
List of operational phases: https://github.com/ldbc/ldbc_spb_bm/wiki/Operational-Phases

  
* ***Configuration options:*** https://github.com/ldbc/ldbc_spb_bm/wiki/Configuration-Options
* ***Definition properties:*** https://github.com/ldbc/ldbc_spb_bm/wiki/Definitions-Properties

 
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