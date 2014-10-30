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

* Benchmark Phases (test.properties)
    One, some or all phases can be enabled and will run in the ordered list below. Running first five phases is mandatory with optionally enabling fourth one (*loadCreativeWorks*) - for the case when generated data will not be loaded manually into the database. Description of phases can be found here: https://github.com/ldbc/ldbc_spb_bm/wiki/Benchmark-Phases

* Conifgure the benchmark driver to :
  * ***Generate Data*** - enable phases : loadOntologies, loadReferenceDatasets, generateCreativeWorks
  * ***Load Generated Data*** - *Generate Data*, enable phase : loadCreativeWorks (generated data can also be loaded manually from folder 'creativeWorksPath/' if database doesn't support automatic loading)
  * ***Generate Query Substitution Parameters*** - *Generate Data* and *Load Generated Data*, enable phase : generateQuerySubstitutionParameters
  * ***Validate Query Results*** - to be executed on an empty database, enable phases : loadOntologies, loadDatasets
  * ***Run The Benchmark*** - *Generate Data*, *Load Generated Data*, *Generate Query Substitution Parameters*, enable phases : warmUp, runBenchmark
  * ***Run Online Replication and Backup Benchmark*** - *Generate Data*, *Load Generated Data*, *Generate Query Substitution Parameters*, enable phase : runBenchmarkOnlineReplicationAndBackup. Also make a full backup prior to running the benchmark for later restore point and implement all scripts in folder 'data/enterprise/scripts/' specific to each database.
  * ***Check Conformance to OWL2-RL Rule-Set*** - to be executed on an empty database with OWL2-RL rule-set, enable phase : loadOntologies. No data generation or loading is required.
  
* Detailed descriptions of configuration options can be found here: https://github.com/ldbc/ldbc_spb_bm/wiki/Configuration-Options
* Detailed descriptions of definitions options can befound here: https://github.com/ldbc/ldbc_spb_bm/wiki/Definitions-properties

  
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