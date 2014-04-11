![LDBC Logo](ldbc_logo.png)
Semantic Publishing Benchmark
-----------------------------

###Description

Semantic Publishing Benchmark is an LDBC benchmark for testing the performance of RDF engines inspired by the Media/Publishing industry.
Performance is measured by producing a workload of CRUD (Create, Read, Update, Delete) operations which are executed simultaneously.

The benchmark offers a data generator that uses real reference data to produce datasets of various sizes and tests the scalability aspect
of RDF systems. The benchmark workload consists of (a) editorial operations that add new data, alter or delete existing (b) aggregation
operations that retrieve content according to various criteria. The aggregation queries define different "choke points", that is technical 
challenges that a standard RDF store must overcome thus giving opportunities for improvement regarding query optimization.

The benchmark also tests conformance for various rules inside the OWL2-RL rule-set.

###Build

Apache Ant build tool is required. Use one of the following tasks : 

```
#to build a standard version of the benchmark, compliant to SPARQL 1.1 standard
ant build-base-querymix-standard

#to build a standard version of the benchmark, compliant to SPARQL 1.1 standard with extended query mix
ant build-full-querymix-standard

#to build a version of the benchmark customized for Virtuoso Database
ant build-base-querymix-virtuoso

#to build a version of the benchmark customized for Virtuoso Database with extended query mix
ant build-full-querymix-virtuoso
```

Result of the build process is saved to the distribution folder (dist/) : 
* semantic_publishing_benchmark-*.jar
* semantic_publishing_benchmark_reference_knowledge_data.zip
* definitions.properties
* test.properties
* readme.txt

###Install

Required configuration files : 

* ***test.properties*** - contains configuration parameters for configuring the benchmark driver
* ***definitions.properties*** - contains values of pre-allocated parameters used by the benchmark. Not to be modified by the regular benchmark user

Extract from file **semantic_publishing_benchmark_reference_knowledge_data.zip** following : 

* ***data/*** - folder containing required reference knowledge (ontologies and data) and query templates

Extract from additinal reference datasets (see project ldbc_semanticpub_bm_additional_datasets). This is an optional step.

* ***Files of type .ttl*** and save to data/datasets folder

All items should be saved in same location with the benchmark jar file.

###Configure

* RDF Repository configuration
  * Use RDFS rule-set
  * Enable context indexing (if available)
  * Enable geo-spatial indexing (if available)

* Benchmark driver configuration. All configuration parameters are stored in properties file (test.properties)

  * ***ontologiesPath*** - path to ontologies from reference knowledge, e.g. ./data/ontologies
  * ***referenceDatasetsPath*** - path to data from reference knowledge, e.g. ./data/datasets
  * ***creativeWorksPath*** - path to generated data, e.g. ./data/generated
  * ***queriesPath*** - path to query templates, e.g. ./data/sparql
  * ***definitionsPath*** - path to definitions.properties file, e.g. ./definitions.properties
  * ***endpointURL*** - URL of SPARQL endpoint provided by the RDF database, e.g. http://localhost:8080/openrdf-sesame/repositories/ldbc
  * ***endpointUpdateURL*** - URL of SPARQL endpoint for update operations, e.g. http://localhost:8080/openrdf-sesame/repositories/ldbc1/statements
  * ***datasetSize*** - size of generated data (triples). Data-generator uses this parameter
  * ***generatedTriplesPerFile*** - number of triples per generated file. Used to split the data generation into a number of files
  * ***queryTimeoutSeconds*** - query timeout in seconds
  * ***systemQueryTimeoutSeconds*** -	system queries timeout, default value 1h
  * ***warmupPeriodSeconds*** - warmup period in seconds
  * ***benchmarkRunPeriodSeconds*** - benchmark period in seconds
  * ***generateCreativeWorksFormat*** - serialization format for generated data. Available options : TriG, TriX, N-Triples, N-Quads, N3, RDF/XML, RDF/JSON, Turtle. Use exact names.
  * ***aggregationAgents*** - number of aggregation agents that will execute a mix of aggregation queries simultaneously
  * ***editorialAgents*** - number of editorial agents that will execute a mix of update operations simultaneously
  * ***dataGeneratorWorkers*** - number of worker threads used by the data generator to produce data
  * ***generatorRandomSeed*** - use it to set the random set for the data generator (default value is 0). e.g. in cases when several benchmark drivers are started in separate processes to generate data - to be used with creativeWorkNextId parameter
  * ***creativeWorkNextId*** - set the next ID for the data generator of Creative Works. When running the benchmark driver in separate processes, to guarantee that generated creative works will not overlap their IDs. e.g. for generating 50M dataset, expected number of Creative Works is ~2.5M and next ID should start at that value
  * ***creativeWorksInfo*** - file name, that will be saved in creativeWorksPath and will contain system info about the generated dataset, e.g. interesting entities, etc.
  * ***querySubstitutionParameters*** - number substitution parameters that will be generated for each query
  
* Benchmark Phases (test.properties)
    One, some or all phases can be enabled and will run in the sequence listed below. Running first three phases is mandatory with optionally enabling fourth one (*loadCreativeWorks*) - for the case when generated data will not be loaded manually into the database.
  
  * ***loadOntologies*** - populate the RDF database with required ontologies (from reference knowledge). It can be done manually by uploading all .ttl files located at : /data/ontologies
  * ***adjustRefDatasetsSizes*** - optional phase, if reference dataset files exist with the extension '.adjustablettl', then for each, a new .ttl file is created with adjusted size depending on the selected size of data to be generated (parameter 'datasetSize' in test.properties file).
  * ***loadReferenceDatasets*** - populate the RDF database with required reference data (from reference knowledge). It can be done manually by uploading all .ttl files located at : /data/datasets
  * ***generateCreativeWorks*** - generate the data used for benchmarking. Data is saved to files of defined size (*generatedTriplesPerFile*) and total number of triples (*datasetSize*)
  * ***generateQuerySubstitutionParameters*** - Controls generation of query substitution parameters which later can be used during the warmup and benchmark phases. For each query a substitution parameters file is created and saved into 'creativeWorksPath' location. If no files are found at that location, queries executed during warmup and benchmark phases are randomly generated.
  * ***loadCreativeWorks*** - load generated data from previous phase into RDF database. Optional phase, verified from N-Quads serialization format
  * ***warmUp*** - runs the aggregation agents for *warmupPeriodSeconds* seconds, results are not collected
  * ***runBenchmark*** - runs the benchmark for *benchmarkRunPeriodSeconds* seconds, results are collected. Editorial and aggregation agents are run simultaneously.
 
* Conformance Validation Phase 
    To be run independently on a new repository (using OWL2-RL rule-set). Required phase before running : *loadOntologies*. No data generation and loading is required.
  * ***checkConformance*** - runs tests for conformance to the **OWL2-RL** rules. 

###Run

```sh
java -jar semantic_publishing_benchmark-*.jar test.properties
```
*Note: appropriate value for java maximum heap size may be required, e.g. -Xmx8G*

###Results
Results of the benchmark are saved to three types of log files :

* ***brief*** - brief log of executed queries, saved in semantic_publishing_benchmark_queries_brief.log
* ***detailed*** - detailed log of executed queries with results, saved in semantic_publishing_benchmark_queries_detailed.log
* ***summary*** - editorial and aggregate operations rate, saved in semantic_publishing_benchmark_results.log