![LDBC Logo](ldbc_logo.png)
Semantic Publishing Benchmark
-----------------------------

###Description

Semantic Publishing Benchmark is an LDBC benchmark for testing the performance of RDF databases inspired by the Media/Publishing industry.
Performance is measured by producing a workload of CRUD (Create, Read, Update, Delete) operations which are executed simultaneously.

The benchmark offers a data generator that uses real reference data to produce datasets of various sizes and tests the scalability aspect
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


* Conifgure the benchmark driver to :
  * ***Generate Data*** - enable phases : loadOntologies, loadReferenceDatasets, generateCreativeWorks
  * ***Load Generated Data*** - *Generate Data*, enable phase : loadCreativeWorks (generated data can also be loaded manually from folder 'creativeWorksPath/' if database doesn't support automatic loading)
  * ***Generate Query Substitution Parameters*** - *Generate Data* and *Load Generated Data*, enable phase : generateQuerySubstitutionParameters
  * ***Validate Query Results*** - to be executed on an empty database, enable phases : loadOntologies, loadDatasets
  * ***Run The Benchmark*** - *Generate Data*, *Load Generated Data*, *Generate Query Substitution Parameters*, enable phases : warmUp, runBenchmark
  * ***Run Online Replication and Backup Benchmark*** - *Generate Data*, *Load Generated Data*, *Generate Query Substitution Parameters*, enable phase : runBenchmarkOnlineReplicationAndBackup. Also make a full backup prior to running the benchmark for later restore point and implement all scripts in folder 'data/enterprise/scripts/' specific to each database.
  * ***Check Conformance to OWL2-RL Rule-Set*** - to be executed on an empty database with OWL2-RL rule-set, enable phase : loadOntologies. No data generation or loading is required.


* Benchmark Phases (test.properties)
    One, some or all phases can be enabled and will run in the sequence listed below. Running first five phases is mandatory with optionally enabling fourth one (*loadCreativeWorks*) - for the case when generated data will not be loaded manually into the database.
  
  * ***loadOntologies*** - populate the RDF database with required ontologies (from reference knowledge). Required phase, can be done manually by uploading all .ttl files located at : /data/ontologies
  * ***loadReferenceDatasets*** - populate the RDF database with required reference data (from reference knowledge). Required phase, can be done manually by uploading all .ttl files located at : /data/datasets
  * ***generateCreativeWorks*** - generate the data used for benchmarking. Data is saved to files of defined size (*generatedTriplesPerFile*) and total number of triples (*datasetSize*). *Requires phases : loadOntologies, loadDatasets.*
  * ***loadCreativeWorks*** - load generated data from previous phase into RDF database, using N-Quads serialization format. Some databases may not be able to run this phase, in that case generated data can be loaded manually.
  * ***generateQuerySubstitutionParameters*** - Controls generation of query substitution parameters which later can be used during the warmup and benchmark phases. For each query a substitution parameters file is created and saved into 'creativeWorksPath' location. If no files are found at that location, queries executed during warmup and benchmark phases will use randomly generated parameters. *Requires phases : loadOntologies, loadDatasets, generateCreativeWorks, loadCreativeWorks.*
  * ***validateQueryResults*** - validate correctness of results for editorial and aggregate operations against a validation dataset. *Requires phases : loadOntologies, loadDatasets.*
  * ***warmUp*** - runs the aggregation agents for *warmupPeriodSeconds* seconds, results are not collected. *Requires phases : loadOntologies, loadDatasets, generateCreativeWorks (optional, in case it has been already done), loadCreativeWorks (optional, in case it has been already done), generateQuerySubstitutionParameters.*
  * ***runBenchmark*** - runs the benchmark for *benchmarkRunPeriodSeconds* seconds, results are collected. Editorial and aggregation agents are run simultaneously. *Requires phases : loadOntologies, loadDatasets, generateCreativeWorks (optional, in case it has been already done), loadCreativeWorks (optional, in case it has been already done), generateQuerySubstitutionParameters.*
  * ***runBenchmarkOnlineReplicationAndBackup*** - benchmark is measuring performance under currently ongoing backup process. Verifies that certain conditions are met such as milestone points at which backup has been started. Requires additional implementation of provided shell script files (/data/enterprise/scripts) for using vendor's specific command for backup. *Requires phases : loadOntologies, loadDatasets, generateCreativeWorks, loadCreativeWorks, generateQuerySubstitutionParameters. Also making a full backup prior to running the benchmark for later restore point.*
  * ***checkConformance*** - runs tests for conformance to the **OWL2-RL** rules. *Requires phase : loadOntologies.*. To be run independently on an empty repository (using OWL2-RL rule-set). Required phase : *loadOntologies*. No data generation and loading is required.


* Detailed configuration. All configuration parameters are stored in properties file (test.properties). Most have default values that are ready to use, others however require updating.

  * ***ontologiesPath*** - path to ontologies from reference knowledge, default: ./data/ontologies
  * ***referenceDatasetsPath*** - path to data from reference knowledge, default: ./data/datasets
  * ***creativeWorksPath*** - path to generated data, default: ./data/generated
  * ***queriesPath*** - path to query templates, default: ./data/sparql
  * ***definitionsPath*** - path to definitions.properties configuration file, default: ./definitions.properties
  * ***endpointURL*** - URL of SPARQL endpoint provided by the RDF database, *requires updating*
  * ***endpointUpdateURL*** - URL of SPARQL endpoint used for update operations, *requires updating*
  * ***datasetSize*** - amount of generated data (triples), *requires updating*
  * ***generatedTriplesPerFile*** - number of triples per generated file. Used to split the data generation into a number of files
  * ***adjustRefDatasetsSizes*** - optional, if reference dataset files exist with the extension '.adjustablettl', then for each, a new .ttl file is created with adjusted size depending on the selected size of data to be generated (parameter 'datasetSize'), default value is true
  * ***allowSizeAdjustmentsOnDataModels*** - allows data generator to dynamically adjust the amount of correlations, clusterrings and randomly generated models keeping a ratio of 1/3 for each in generated data model. This property overrides definitions.properties' parameters : *majorEvents, minorEvents, correlationsAmount*. Default value is true
  * ***queryTimeoutSeconds*** - query timeout in seconds, default value is 300 s
  * ***systemQueryTimeoutSeconds*** -	system queries timeout, default value 1h
  * ***validationPath*** - location where generated and reference data related to validation phase is located, can use default value
  * ***generateCreativeWorksFormat*** - serialization format for generated data. Available options : TriG, TriX, N-Triples, N-Quads, N3, RDF/XML, RDF/JSON, Turtle. Use exact names. Required are context aware serialization formats such as: N-Quads, TriX, TriG  
  * ***warmupPeriodSeconds*** - warmup period in seconds, *requires updating*
  * ***benchmarkRunPeriodSeconds*** - benchmark period in seconds, *requires updating*
  * ***aggregationAgents*** - number of aggregation agents that will execute a mix of aggregation queries simultaneously, *requires updating*
  * ***editorialAgents*** - number of editorial agents that will execute a mix of update operations simultaneously, *requires updating*
  * ***dataGeneratorWorkers*** - number of worker threads used by the data generator to produce data, *requires updating*
  * ***generatorRandomSeed*** - use it to set the random seed for the data generator (default value is 0). e.g. in cases when several benchmark drivers are started in separate processes to generate data - to be used with creativeWorkNextId parameter
  * ***creativeWorkNextId*** - sets the next ID of Creative Works. When running the benchmark driver to generate synthetic data in separate processes, in order to guarantee that all generated creative works will not overlap by their IDs, add an increment in value ~ 2.6M for each 50M generated triples
  * ***creativeWorksInfo*** - name of a file containing system info about the generated dataset, e.g. interesting entities, etc. (will be saved in 'creativeWorksPath')
  * ***querySubstitutionParameters*** - number substitution parameters that will be generated for each query, default value is 100000
  * ***benchmarkByQueryRuns*** - sets the amount of aggregate queries which the benchmark phase will execute. If value is greater than zero then parameter 'benchmarkRunPeriodSeconds' is ignored. e.g. if set to 100, benchmark will measure the time to execute 100 aggregate operations
  * ***updateRateThresholdOps*** - defines the update rate of editorial operations per second which should be reached during the first 15% of benchmark time and should be kept during the rest of the benchmark run in order to have a valid result. If set to zero, update rate threshold is ignored. e.g. if required update rate is set to 6.3 update operations per second, then benchmark will consider that value during its benchmark run and will report invalid results if that rate drops below the threshold
  * ***updateRateThresholdReachTimePercent*** - defines the time frame during which the defined value in property 'updateRateThresholdOps' should be reached. Default value is 0.1 (10%). e.g. if set to 0.1 (i.e. 10%) then the update rate defined in 'updateRateThresholdOps' should be reached during the first 10% of the benchmark run time, if not reached, the result is considered invalid
  
  
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