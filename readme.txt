Semantic Publishing Benchmark



Description : 
------------------------------------------------------------------------------

The benchmark driver measures the performance of CRUD operations of a SPARQL endpoint, by starting a number of concurrently running agents (editorial and aggregation) 
which execute a series of INSERT/UPDATE/DELETE (for editorial agents) and CONSTRUCT/SELECT (for aggregation agents) queries on a SPARQL endpoint.



Distribution : 
------------------------------------------------------------------------------

The benchmark test driver is distributed as a jar file : semantic_publishing_benchmark-*.jar accompanied by a datasets archive file : reference_knowledge_data.zip
The datasets file contains ontologies and reference datasets (required by the data-generator). Benchmark jar file contains configuration files : test.properties and definitions.properties. 



Project Structure : 
------------------------------------------------------------------------------

src\            			      (sources of the project)
dist\           			      (the benchmark driver jar file)
bin\            			      (binary files)    
data\           			      (contains required ontologies and reference datasets used by the data generator. Also will be packed with the distribution jar)
  ontologies\   			      (ontologies, core and domain, describing a publishing use-case)
    core\
    domain\
  datasets\     			      (reference datasets, used by the data-generator for generating synthetic data for benchmarking)
  sparql\       			      (query templates and constraint validation queries)
  	aggregation\			      (aggregation query templates)
  	conformance\ 				  (queries for testing OWL2-RL conformance capabilities of the database engine)
  	editorial\				      (editorial query templates)



How to build the benchmark driver :
-----------------------------------------------------------------------------------------------------------------------------------------------

  Use the Ant with build.xml script. Default Ant task builds the jar and saves it to the 'dist' folder.
  Currently two versions of the Benchmark exist : a base version - containing a reduced query-mix with 9 queries and advanced version with 26 queries,
  use appropriate ant-tasks to build them, e.g.
  > ant build-base-querymix-standard //builds the standard benchmark driver compliant to SPARQL 1.1
  > ant build-base-querymix-virtuoso // builds a custom version of the driver customized for Virtuoso's small deviation in SPARQL queries
  > ant build-full-querymix-standard
  > ant build-full-querymix-virtuoso



How to install the benchmark driver :
-----------------------------------------------------------------------------------------------------------------------------------------------

Save the distribution jar and reference knowledge data files to a folder of choice, then extract from both following items :
    - folder data/ from reference knowledge data file - contains required ontologies, knowledge reference data and query templates
    - additinal reference datasets (see project ldbc_semanticpub_bm_additional_datasets) - all files of type .ttl and save to data/datasets folder    
    - file test.properties - configuration parameters for running the benchmark, found in the benchmark jar file
    - file definitions.properties - configuration parameters on the benchmark generator, found in the benchmark jar file



Benchmark Phases : 
-----------------------------------------------------------------------------------------------------------------------------------------------

  * The Semantic Publishing Benchmark can be configured to run through these phases ordered by the sequence they should be run : 

    - loadOntologies        		: load ontologies (from the 'data/ontologies' folder) into database
    - adjustRefDatasetsSizes    	: optional phase, if reference dataset files exist with the extension '.adjustablettl', then for each, a new .ttl file
                                 	  is created with adjusted size depending on the selected size of data to be generated (parameter 'datasetSize' in test.properties file).
    - loadDatasets          		: load the reference datasets (from the 'data/datasets' folder) into database
    - generateCreativeWorks 		: using uploaded data from previous two phases, generates Creative Works and saves them to files.
                                  Generated files need to be loaded into database manually (or automatically if file format is n-quads)
                                  Note : in order to execute generateCreativeWorks phase, ontologies and reference data from previous two phases must be present in database
    - loadCreativeWorks	  		  : load generated creative works into database (Tested for N-Quads)
    - warmUp                		: a series of Aggregation queries are executed for a fixed amount of time.
    - benchmark             		: all aggregation and editorial agents are started and kept running for a period of 'benchmarkRunPeriodSeconds'.
    - checkConformance          : executes predefined queries (from folder 'data/sparql/conformance'. Checking for OWL2-RL : prp-irp, prp-asyp, prp-pdw, prp-adp, cax-dw, cax-adc, cls-maxc1, prp-key, prp-spo2, prp-inv1)  
                                  Note : in order to execute generateCreativeWorks phase, ontologies from loadOntologies phase must be present in database
    - cleanup               		: optional, the benchmark can be set to clear all data from database
                                  Note : all data will be erased from the repository
  
    Each of those phases can be configured to run independently or in a sequence by setting appropriate property value in file : test.properties.
 
 
 
How to run the benchmark : 
-----------------------------------------------------------------------------------------------------------------------------------------------

  * Prepare and start a new RDF repository. 
  
    - Use rule-set : RDFS
    - Enable context indexing if available
    - Enable geo-spatial indexing if available
  
  * Configure the benchmark driver
  
      Edit file : test.properties, set values for :
  
    - ontologiesPath                    (ontologies path, e.g. "./data/ontologies")
    - referenceDatasetsPath             (reference dataset path, e.g. "./data/datasets")
    - creativeWorksPath                 (generated creative works path, e.g. "./data/generated")
    - queriesPath                       (queries path, e.g. "./data/sparql")
    - definitionsPath                   (definitions path, e.g. "./definitions.properties")
    - endpointURL                       (URL of endpoint, e.g. "http://localhost:8080/openrdf-sesame/repositories/ldbc1")
    - endpointUpdateURL                 (URL of endpoint for executing update queries, e.g. "http://localhost:8080/openrdf-sesame/repositories/ldbc1/statements")
    - datasetSize                       (target dataset size in triples, number of triples that will be generated by the data-generator)
    - generatedTriplesPerFile           (generated triples per file)
    - queryTimeoutSeconds               (query timeout)
    - systemQueryTimeoutSeconds			(system queries timeout, default value 1h)
    - warmupPeriodSeconds               (warmup period)
    - benchmarkRunPeriodSeconds         (benchmark run period)
    - generateCreativeWorksFormat       (available options: TriG, TriX, N-Triples, N-Quads, N3, RDF/XML, RDF/JSON, Turtle)
    - aggregationAgents                 (aggregation agents count which will execute mix of aggregation queries concurrently. Query mix can be configured by changing 
                                         parameter aggregationOperationsAllocation in definitions.properties file)
    - editorialAgents                   (editorial agents count which will execute a mix of editorial queries concurrently. Query mix can be configured by changing
                                         parameter editorialOperationsAllocation in definitions.properties file)
    - dataGeneratorWorkers              (number of simultaneously working data generator threads)
                                         
                                         Note : For optimal results the sum of editorial and aggregation agents should be set to be equal to the number of CPU cores.
	
      (Configure the benchmark phases. One, several or all phases can be enabled to run in a sequence. Running the first three phases is mandatory for the benchmark )
      
    - loadOntologies                    (populate the database with required ontologies, it is possible to manually upload the data stored in all .ttl files at /data/ontologies)
    - loadReferenceDatasets             (populate the database with required reference datasets, it is possible to manually upload the data stored in all .ttl files at /data/datasets)
    - generateCreativeWorks             (using already loaded ontologies and reference datasets, generate the benchmark data (Creative Works) into files)
    - loadCreativeWorks                 (load generated files with Creative Works into repository, optional, tested for N-Quads)
    - warmUp                            (runs the aggregation queries for a configured period of time)
    - runBenchmark                      (runs the benchmark - all aggregation and editorial agents run simultaneously)
    - checkConformance                  (executes a set of queries stored in 'data/sparql/conformance' for testing the inference capabilities of the database engine.
                                        OWL2-RL : prp-irp, prp-asyp, prp-pdw, prp-adp, cax-dw, cax-adc, cls-maxc1, prp-key, prp-spo2, prp-inv1.
                                        Note : execute -loadOntologies phase before running conformance check
    - clearDatabase                     (erases all triples from database)
	 
      Sample of a test.properties file can be found in the distribution jar file.

  * definitions.properties - currently pre-configured and no need to modify. Can be edited to tune various allocations configurations, used in -generateCreativeWorks and -runBenchmark phases.
  
    - aboutsAllocations                 (Defines allocation amount of About tags in Creative Works)
    - mentionsAllocations               (Defines allocation amount of Mention tags in Creative Works)
    - entityPopularity                  (Defines popularity of an entity in the reference datasets)
    - usePopularEntities                (Defines allocation amount of popular entities to be used when tagging in Creative Works or in aggregation queries. Used for generation of Creative Works biased towards popular entities)
    - creativeWorkTypesAllocation       (Defines allocation amount of Creative Work Types : BlogPost, NewsItem, Programme)
    - aboutAndMentionsAllocation        (Defines allocation amount of about or mentions used for the main aggregation query (/data/sparql/aggregation/query1.txt), which one will be used more frequently)
    - editorialOperationsAllocation     (Defines allocation amount of queries in the editorial query mix that each editorial agent will execute. Query mix order : insert.txt, update.txt and delete.txt)
    - aggregationOperationsAllocation   (Defines allocation amount of queries in the aggregation query mix that each aggregation agent will execute. Query mix order : query1.txt, query2.txt... etc)
    
      Sample definitions.properties file can be found in the distribution jar file.

  * Example benchmark run command : 

  	  java -jar semantic_publishing_benchmark-*.jar test.properties
  	  
  	  Note: appropriate value for java maximum heap size may be required, e.g. -Xmx4096m



Results of the benchmark :
-----------------------------------------------------------------------------------------------------------------------------------------------

  * Results are saved to three log files : 
  
    - semantic_publishing_benchmark_queries_brief.log 		- contains a brief information about each executed query, size of returned result, and time to execute.
    - semantic_publishing_benchmark_queries_detailed.log 	- contains a detailed log of each query and its result.
    - semantic_publishing_benchmark_results.log 			    - contains results from the the benchmark, saved each second during the run.