LDBC Semantic Publishing Benchmark
----------------------------------

**Description**

Semantic Publishing Benchmark is an LDBC benchmark for testing the performance an RDF engines inspired by the Media/Publishing industry.
Performance is measured by producing a workload of CRUD (Create, Read, Update, Delete) operations which are executed simultaneously.

The benchmark offers a data generator that uses real reference data to produce datasets of various sizes and tests the scalability aspect
of RDF systems. The benchmark workload consists of (a) editorial operations that add new data, alter or delete existing (b) aggregation
operations that retrieve content according to various criteria. The aggregation queries define different "choke points", that is technical 
challenges that a standard RDF store must overcome thus giving opportunities for improvement regarding query optimization.

The benchmark also tests conformance for various rules inside the OWL2-RL rule-set.

**Build**

Apache Ant build tool is required. Use one of the tasks to build appropriate version : 

* ant build-base-querymix-standard - to build standard version of the benchmark compliant to SPARQL 1.1 standard
* ant build-base-querymix-virtuoso - to build a version of the benchmark customized for Virtuoso 7 Database

**Install**

Extract from the benchmark jar file following items :
(all items to be saved at same location of the benchmark jar)

* test.properties - contains configuration parameters for configuring the benchmark driver
* definitions.properties - contains values of pre-allocated parameters. Not to be changed for a regular benchmark use
* data/ - folder containing required reference knowledge (ontologies and data) and query templates

**Configure**

* Create a new RDF database
  * Use RDFS rule-set
  * Enable context indexing (if available)
  * Enable geo-spatial indexing (if available)

* Benchmark driver configuration (set values in file : test.properties)
  * [ontologiesPath] - path to ontologies from reference knowledge, e.g. "./data/ontologies"
  * [referenceDatasetsPath] - path to data from reference knowledge, e.g. "./data/datasets"
  * [creativeWorksPath] - path to generated data, e.g. "./data/generated"
  * [queriesPath] - path to query templates, e.g. "./data/sparql"
  * [definitionsPath] - path to definitions.properties file, e.g. "./definitions.properties"
  * [endpointURL] - URL of SPARQL endpoint provided by the RDF database, e.g. "http://localhost:8080/openrdf-sesame/repositories/ldbc"
  * [endpointUpdateURL] - URL of SPARQL endpoint for update operations, e.g. "http://localhost:8080/openrdf-sesame/repositories/ldbc1/statements"
  * [datasetSize] - size of generated data (triples). Data-generator uses this parameter
  * [generatedTriplesPerFile] - number of triples per generated file. Used to split the data generation into a number of files
  * [queryTimeoutSeconds] - query timeout in seconds
  * [warmupPeriodSeconds] - warmup period in seconds
  * [benchmarkRunPeriodSeconds] - benchmark period in seconds
  * [generateCreativeWorksFormat] - serialization format for generated data. Available options : TriG, TriX, N-Triples, N-Quads, N3, RDF/XML, RDF/JSON, Turtle. Use exact names.
  * [aggregationAgents] - number of aggregation agents that will execute a mix of aggregation queries simultaneously
  * [editorialAgents] - number of editorial agents that will execute a mix of update operations simultaneously
  
  Benchmark phases :
    One, some or all phases can be enabled and will run in sequence shown below. Running first three phases is mandatory for the [runBenchmark] and optionally forth ([loadCreativeWorks]) in cases when generated data will not be loaded manually.
  
  * [loadOntologies] - populate the RDF database with required ontologies (from reference knowledge). It can be done manually by uploading all .ttl files located at : /data/ontologies
  * [loadReferenceDatasets] - populate the RDF database with required reference data (from reference knowledge). It can be done manually by uploading all .ttl files located at : /data/datasets
  * [generateCreativeWorks] - generate the data used for benchmarking. Data is saved to files of defined size ([generatedTriplesPerFile]) and total number of triples ([datasetSize])
  * [loadCreativeWorks] - load generated data from previous phase into RDF database. Optional phase, verified from N-Quads serialization format
  * [warmUp] - runs the aggregation agents for [warmupPeriodSeconds] seconds, results are not collected
  * [runBenchmark] - runs the benchmark for [benchmarkRunPeriodSeconds] seconds, results are collected. Editorial and aggregation agents are run simultaneously.
  
  Conformance phase :
    To be run independently on a new repository (using OWL2-RL rule-set). Required phase before running : [loadOntologies]. No data generation and loading is required.
  * [checkConformance] - runs tests for conformance to the **OWL2-RL** rules. 

**Run**

```sh
java -jar semantic_publishing_benchmark-*.jar test.properties
```

**Results**
Results of the benchmark are saved to three types of log files :
* brief log - semantic_publishing_benchmark_queries_brief.log
* detailed log - semantic_publishing_benchmark_queries_detailed.log
* benchmark results - results of the benchmark