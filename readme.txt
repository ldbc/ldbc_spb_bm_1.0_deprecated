Semantic Publishing Benchmark



Description : 
------------------------------------------------------------------------------

Semantic Publishing Benchmark benchmark driver measures the performance of CRUD operations of a SPARQL endpoint, by starting a number of concurrently running agents (editorial and aggregation) 
which execute a series of INSERT/UPDATE/DELETE (for editorial agents) and CONSTRUCT/SELECT (for aggregation agents) queries on a SPARQL endpoint.



Distribution : 
------------------------------------------------------------------------------

The benchmark test driver is distributed as a jar file : semantic_publishing_benchmark-*.jar accompanied by reference datasets and ontologies.
Required set of configuration files are : test.properties and definitions.properties (can be found in the distribution folder, or semantic_publishing_benchmark-*jar file) 



How to build the benchmark driver :
-----------------------------------------------------------------------------------------------------------------------------------------------

  Use the Ant with build.xml script. Default Ant task builds the benchmark and saves it to the 'dist' folder.
  Currently two versions of the Benchmark exist : a basic version - containing a reduced query-mix with 9 queries and advanced version with 25 queries,
  use appropriate ant-tasks to build them, e.g.
  
  $ ant build-basic-querymix          //builds the standard benchmark driver compliant to SPARQL 1.1
  $ ant build-advanced-querymix          //builds the standard benchmark driver compliant to SPARQL 1.1 with extended query mix
  $ ant build-basic-querymix-virtuoso //builds a custom version of the driver customized for Virtuoso's small deviation in SPARQL queries
  $ ant build-advanced-querymix-virtuoso //builds a custom version of the driver customized for Virtuoso's small deviation in SPARQL queries with extended query mix



How to install the benchmark driver :
-----------------------------------------------------------------------------------------------------------------------------------------------

All necessary files required to run the benchmark are saved to the 'dist' folder. You can run the benchmark from it or move it to a new location.

Optionally you can install the additinal reference datasets :
    - download project ldbc_semanticpub_bm_additional_datasets from https://github.com/ldbc/ldbc_semanticpub_bm_additional_datasets and unzip all files to folder data/datasets    


Benchmark Phases : 
-----------------------------------------------------------------------------------------------------------------------------------------------

  * The Semantic Publishing Benchmark can be configured to run through these phases ordered by the sequence they should be run : 

    - loadOntologies        		          		: load ontologies (from the 'data/ontologies' folder) into database. It can be done manually by uploading all .ttl files located at : /data/ontologies into the database
    - loadDatasets          		          		: load the reference datasets (from the 'data/datasets' folder) into database. It can be done manually by uploading all .ttl files located at : /data/ontologies into the database
    - generateCreativeWorks 		          		: using uploaded data from previous two phases, generates Creative Works and saves them to files. Generated files need to be loaded into database manually (or automatically if file format is n-quads)
                                              			Note: Requires phases : loadOntologies, loadDatasets.
    - loadCreativeWorks	  		            		: load generated creative works into database (It is advisable to use serialization format : N-Quads)
    - generateQuerySubstitutionParameters 		: Controls generation of query substitution parameters which later can be used during the warmup and benchmark phases. For each query a substitution parameters file is created and saved into 'creativeWorksPath' location. 
                                              			Note : If no files are found at that location, queries executed during warmup and benchmark phases will use randomly generated parameters.
                                              			Note2: Requires phases : loadOntologies, loadDatasets, generateCreativeWorks, loadCreativeWorks.
    - validateQueryResults                		: validate correctness of results for editorial and aggregate operations against a validation dataset.
                                              			Note : Requires phases : loadOntologies, loadDatasets.
    - warmUp                		          		: a series of Aggregation queries are executed for a fixed amount of time.
    - benchmark             		          		: all aggregation and editorial agents are started and kept running for a period of 'benchmarkRunPeriodSeconds'.
    - benchmarkOnlineReplicationAndBackup 		: benchmark is measuring performance under currently ongoing backup process. Verifies that certain conditions are met such as milestone points at which backup has been started. 
                                            			Note : Requires phases : loadOntologies, loadDatasets, generateCreativeWorks, loadCreativeWorks, warmUp (optional). Phases that should be disabled : benchmark.
                                            			Note2: Requires all necessary enterprise script files (data/enterprise/scripts) to have DB Engie's commands added (Commands for : starting, shutting down, backing up, etc).
                                            			Note3: Required to set the full path for property 'enterpriseFeaturesPath' in test.properties file and all scripts need to have an execution permission enabled.  
    - checkConformance                    		: executes predefined queries (from folder 'data/sparql/conformance'. Checking for OWL2-RL : prp-irp, prp-asyp, prp-pdw, prp-adp, cax-dw, cax-adc, cls-maxc1, prp-key, prp-spo2, prp-inv1)  
                                              			Note : Requires phase : loadOntologies.
    - cleanup               		          		: optional, the benchmark can be set to clear all data from database
                                              			Note : all data will be erased from repository
  
    Each of those phases can be configured to run independently or in a sequence by setting appropriate property value in file : test.properties.
 
 
 
How to run the benchmark : 
-----------------------------------------------------------------------------------------------------------------------------------------------

  * Prepare and start a new RDF repository. 
  
    - Use rule-set : RDFS
    - Enable context indexing if available
    - Enable geo-spatial indexing if available
  
    
  * Conifgure the benhcmark driver to :
  	 - Generate Data - enable phases : loadOntologies, loadReferenceDatasets, generateCreativeWorks
     - Load Generated Data - *Generate Data*, enable phase : loadCreativeWorks (generated data can also be loaded manually from folder 'creativeWorksPath/' if database doesn't support automatic loading)
     - Generate Query Substitution Parameters - *Generate Data* and *Load Generated Data*, enable phase : generateQuerySubstitutionParameters
     - Validate Query Results - to be executed on an empty database, enable phases : loadOntologies, loadDatasets
     - Run The Benchmark - *Generate Data*, *Load Generated Data*, *Generate Query Substitution Parameters*, enable phases : warmUp, runBenchmark
     - Run Online Replication and Backup Benchmark - *Generate Data*, *Load Generated Data*, *Generate Query Substitution Parameters*, enable phase : runBenchmarkOnlineReplicationAndBackup. Also make a full backup prior to running the benchmark for later restore point and implement all scripts in folder 'data/enterprise/scripts/' specific to each database.
     - Check Conformance to OWL2-RL Rule-Set - to be executed on an empty database with OWL2-RL rule-set, enable phase : loadOntologies. No data generation or loading is required.    
  
  
  (Configure the benchmark phases. One, several or all phases can be enabled to run in a sequence. Running the first three phases is mandatory for the benchmark )
      
    - loadOntologies                    (populate the database with required ontologies, it is possible to manually upload the data stored in all .ttl files at /data/ontologies)
    - loadReferenceDatasets             (populate the database with required reference datasets, it is possible to manually upload the data stored in all .ttl files at /data/datasets)
    - generateCreativeWorks             (using already loaded ontologies and reference datasets, generate the benchmark data (Creative Works) into files)
    - loadCreativeWorks                 (load generated files with Creative Works into repository, optional, tested for N-Quads)
    - warmUp                            (runs the aggregation queries for a configured period of time)
    - runBenchmark                      (runs the benchmark - all aggregation and editorial agents run simultaneously)
    - runBenchmarkOnlineReplicationAndBackup (benchmark is measuring performance under currently ongoing backup process. Verifies that certain conditions are met such as milestone points at which backup has been started. Requires additional implementation of provided shell script files (/data/enterprise/scripts) for using vendor's specific command for backup.)     
    - checkConformance                  (executes a set of queries stored in 'data/sparql/conformance' for testing the inference capabilities of the database engine.
                                        OWL2-RL : prp-irp, prp-asyp, prp-pdw, prp-adp, cax-dw, cax-adc, cls-maxc1, prp-key, prp-spo2, prp-inv1.
                                        Note : execute -loadOntologies phase before running conformance check
    - clearDatabase                     (erases all triples from database)
	 
      Sample of a test.properties file can be found in the distribution folder.
  
  
  * Detailed configuration of the benchmark driver
  
      Edit file : test.properties, All configuration parameters are stored in test.properties file. Most have default values that are ready to use, others however require updating.
  
    - ontologiesPath                    (path to ontologies from reference knowledge, default: ./data/ontologies)
    - referenceDatasetsPath             (path to data from reference knowledge, default: ./data/datasets)
    - creativeWorksPath                 (path to generated data, default: ./data/generated)
    - queriesPath                       (path to query templates, default: ./data/sparql)
    - definitionsPath                   (path to definitions.properties configuration file, default: ./definitions.properties)
    - endpointURL                       (URL of SPARQL endpoint provided by the RDF database, *requires updating*)
    - endpointUpdateURL                 (URL of endpoint for executing update queries, *requires updating*)
    - datasetSize                       (amount of generated data (triples), *requires updating*)
    - generatedTriplesPerFile           (number of triples per generated file. Used to split the data generation into a number of files)    
    - adjustRefDatasetsSizes    	      (optional, if reference dataset files exist with the extension '.adjustablettl', then for each, a new .ttl file is created with adjusted size depending on the selected size of data to be generated (parameter 'datasetSize'), default value is true)    
    - allowSizeAdjustmentsOnDataModels  (allows the data generator to adjust the amount of correlations, clusterings and randomly generated models (Creative Works) in relation to the 'datasetSize', thus keeping a ratio of 1/3 for each in generated data. Default value is true  
    - queryTimeoutSeconds               (query timeout in seconds, default value is 300 s)
    - systemQueryTimeoutSeconds			    (system queries timeout, default value 1h)
    - validationPath                    (location where generated and reference data related to validation phase is located, can use default value)
    - generateCreativeWorksFormat       (serialization format for generated data. Available options : TriG, TriX, N-Triples, N-Quads, N3, RDF/XML, RDF/JSON, Turtle. Use exact names. Required are context aware serialization formats such as: N-Quads, TriX, TriG)    
    - generatedTriplesPerFile           (generated triples per file, sets the number of triples per file)
    - warmupPeriodSeconds               (warmup period, *requires updating*)
    - benchmarkRunPeriodSeconds         (benchmark run period, *requires updating*)
    - aggregationAgents                 (number of aggregation agents that will execute mix of aggregation queries simultaneously, *requires updating*)
    - editorialAgents                   (number of editorial agents that will execute a mix of editorial queries simultaneously, *requires updating*)
    - dataGeneratorWorkers              (number of worker threads used by the data generator to produce data, *requires updating*)
    - generatorRandomSeed				        (use it to set the random set for the data generator (default value is 0). e.g. in cases when several benchmark drivers are started in separate
                                         processes to generate data - to be used with creativeWorkNextId parameter)
    - creativeWorkNextId                (sets the next ID of Creative Works. When running the benchmark driver to generate synthetic data in separate processes, in order to guarantee that all generated creative works will not overlap by their IDs, add an increment in value ~ 2.6M for each 50M generated triples)
    - creativeWorksInfo                 (name of file that contains system info about the generated dataset, e.g. interesting entities, etc. (will be saved in 'creativeWorksPath'))
    - querySubstitutionParameters       (number substitution parameters that will be generated for each query, default value is 100000)
    - benchmarkByQueryRuns				      (sets the amount of aggregate queries which the benchmark phase will execute. If value is greater than zero then parameter 'benchmarkRunPeriodSeconds' is ignored. e.g. if set to 100, benchmark will measure the time to execute 100 aggregate operations.)
    - updateRateThresholdOps        	  (defines the update rate of editorial operations per second which should be reached during the first 15% of benchmark time and should be kept during the rest of the benchmark run in order to have a valid result. If set to zero, update rate threshold is ignored.
                                         e.g. if required update rate is set to 6.3 update operations per second, then benchmark will consider that value during its benchmark run and will report invalid results if that rate drops below the threshold)
    - updateRateThresholdReachTimePercent (defines the time frame during which the defined value in property 'updateRateThresholdOps' should be reached. Default value is 0.1 (10%)
                                         e.g. if set to 0.1 (i.e. 10%) then the update rate defined in 'updateRateThresholdOps' should be reached during the first 10% of the benchmark run time, if not reached, the result is considered invalid)
	- enableLogs						 (enables or disables logging of benchmark results and details, default: true)                   									
                                         
                                         Note : For optimal results the sum of editorial and aggregation agents should be set to be equal to the number of CPU cores.
		
  * definitions.properties - currently pre-configured and no need to modify. Can be edited to tune various allocations, used in -generateCreativeWorks and -runBenchmark phases.
  
    - aboutsAllocations                 (Defines allocation amount of About tags in Creative Works)
    - mentionsAllocations               (Defines allocation amount of Mention tags in Creative Works)
    - entityPopularity                  (Defines popularity of an entity in the reference datasets)
    - usePopularEntities                (Defines allocation amount of popular entities to be used when tagging in Creative Works or in aggregation queries. Used for generation of Creative Works biased towards popular entities)
    - creativeWorkTypesAllocation       (Defines allocation amount of Creative Work Types : BlogPost, NewsItem, Programme)
    - aboutAndMentionsAllocation        (Defines allocation amount of about or mentions used for the main aggregation query (/data/sparql/aggregation/query1.txt), which one will be used more frequently)
    - editorialOperationsAllocation     (Defines allocation amount of queries in the editorial query mix that each editorial agent will execute. Query mix order : insert.txt, update.txt and delete.txt)
    - aggregationOperationsAllocation   (Defines allocation amount of queries in the aggregation query mix that each aggregation agent will execute. Query mix order : query1.txt, query2.txt... etc)
    - exponentialDecayUpperLimitOfCWs   (Defines the maximum number of creative works that an entity can be tagged about. Exponential decay function will start from the value defined)
    - exponentialDecayRate              (Defines the exponential decay rate. Used values to be in range 0.01 (for gentle slope) to 1 (for steep slope))
    - exponentialDecayThresholdPercent  (Defines the threshold in percents of exponential decay, below that threshold values will be ignored. Threshold is defined as the ratio of : currentExponentialDecayValue / exponentialDecayUpperLimitOfCWs. e.g. 5% threshold will be the value of 0.05)
    - majorEventsPerYear                (Defines the maximum number of 'major' events that could happen in one year. Each major event will be tagged by a number of Creative Works which will decay exponentially in time.)
    - minorEventsPerYear                (Defines the maximum number of 'minor' events that could happen in one year. Each minor event will be tagged by a number of Creative Works which will decay exponentially in time. Value of exponentialDecayUpperLimitOfCWs for minor events will be ten times smaller for them.)
    - seedYear                          (Defines a seed year that will be used for generating the Creative Works. Each Creative Work will have its creation date during that year. All date-range queries will use that value also.)
    - dataGenerationPeriodYears			    (Defines the period (in years) of the gnerated data, starting from 'seedYear')
    - correlationsAmount				        (Defines the amount of correlations that data generator will model between entities from reference knowledge data. Default value (50) will produce around 15 million triples.)
    - correlationsMagnitude				      (Defines maximum amount of Creative Works that will be generated for a particular correlation in a single day.)
    - correlationDuration				        (Defines the duration of correlation between two entities as a percent of the total data generation period. Default generation period is one year.)
    - correlationEntityLifespan			    (Defines the life span of each entity that participates in a correlation as a percent of the total data generation period.)
    - minLat							              (Defines minimum latitude, a geo-spatial property used to configure the geo-spatial search area of queries.)
    - maxLat							              (Defines maximum latitude, a geo-spatial property.)
    - minLong							              (Defines minimum longtitude, ,a geo-spatial property.)
    - maxLong							              (Defines maximum longtitude, a geo-spatial property.)    
  	- mileStoneQueryPosition            (Defines the position in terms of percents at which a milestone query is executed (related to Online and Replication Benchmark feature))
    
      Sample definitions.properties file can be found in the distribution folder.

  * Example command to start the benchmark : 

  	  java -jar semantic_publishing_benchmark-*.jar test.properties
  	  
  	  Note: appropriate value for java maximum heap size may be required, e.g. -Xmx8G



Results of the benchmark :
-----------------------------------------------------------------------------------------------------------------------------------------------

  * Results are saved to three log files : 
  
    - semantic_publishing_benchmark_queries_brief.log 		- contains a brief information about each executed query, size of returned result, and time to execute.
    - semantic_publishing_benchmark_queries_detailed.log 	- contains a detailed log of each query and its result.
    - semantic_publishing_benchmark_results.log 			    - contains results from the the benchmark, saved each second during the run.