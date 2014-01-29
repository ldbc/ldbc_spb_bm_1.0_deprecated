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

#to build a version of the benchmark customized for Virtuoso Database
ant build-base-querymix-virtuoso
```

###Install

Extract from benchmark's jar file following items : 

* ***test.properties*** - contains configuration parameters for configuring the benchmark driver
* ***definitions.properties*** - contains values of pre-allocated parameters used by the benchmark. Not to be changed for a regular benchmark use

Extract from reference knowledge data archive file (reference_knowledge_data.zip) following items : 

* ***data/*** - folder containing required reference knowledge (ontologies and data) and query templates

Extract from additinal reference datasets (see project ldbc_semanticpub_bm_additional_datasets) 

* ***Files of type .ttl and save to data/datasets folder***

All items should be saved in same location where the benchmark jar file is.

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
  
* Benchmark Phases (test.properties)
    One, some or all phases can be enabled and will run in a sequence below. Running first three phases is mandatory with optional forth (*loadCreativeWorks*)  in cases when generated data will not be loaded manually.
  
  * ***loadOntologies*** - populate the RDF database with required ontologies (from reference knowledge). It can be done manually by uploading all .ttl files located at : /data/ontologies
  * ***adjustRefDatasetsSizes*** - optional phase, if reference dataset files exist with the extension '.adjustablettl', then for each, a new .ttl file is created with adjusted size depending on the selected size of data to be generated (parameter 'datasetSize' in test.properties file).
  * ***loadReferenceDatasets*** - populate the RDF database with required reference data (from reference knowledge). It can be done manually by uploading all .ttl files located at : /data/datasets
  * ***generateCreativeWorks*** - generate the data used for benchmarking. Data is saved to files of defined size (*generatedTriplesPerFile*) and total number of triples (*datasetSize*)
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
*Note: appropriate value for java maximum heap size may be required, e.g. -Xmx4096m*

###Results
Results of the benchmark are saved to three types of log files :

* ***brief*** - brief log of executed queries, saved in semantic_publishing_benchmark_queries_brief.log
* ***detailed*** - detailed log of executed queries with results, saved in semantic_publishing_benchmark_queries_detailed.log
* ***summary*** - editorial and aggregate operations rate, saved in semantic_publishing_benchmark_results.log