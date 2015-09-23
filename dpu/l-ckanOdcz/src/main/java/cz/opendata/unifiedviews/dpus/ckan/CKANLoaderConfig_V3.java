package cz.opendata.unifiedviews.dpus.ckan;

import eu.unifiedviews.helpers.dpu.ontology.EntityDescription;
/**
 *
 * Put your DPU's configuration here.
 *
 */
@EntityDescription.Entity(type = "http://www.w3.org/ns/dcat#Dataset")
public class CKANLoaderConfig_V3  {
    
   
    public enum Licenses {
    	pddl {
    		//Open Data Commons Public Domain Dedication and License (PDDL)
    		//http://opendefinition.org/licenses/odc-pddl
    		public String toString() {
    			return "odc-pddl" ;
    		}
    	},
    	ccby {
    		//Creative Commons Attribution
    		//http://opendefinition.org/licenses/cc-by
    		public String toString() {
    			return "cc-by" ;
    		}
    	},
    	ccbysa {
    		//Creative Commons Attribution Share-Alike
    		//http://opendefinition.org/licenses/cc-by-sa
    		public String toString() {
    			return "cc-by-sa" ;
    		}
    	},
    	cczero {
    		//Creative Commons CCZero
    		//http://opendefinition.org/licenses/cc-zero
    		public String toString() {
    			return "cc-zero" ;
    		}
    	},
    	ccnc {
    		//Creative Commons Non-Commercial (Any)
    		public String toString() {
    			return "cc-nc" ;
    		}
    	},
    	gfdl {
    		//GNU Free Documentation License
    		public String toString() {
    			return "cc-nc" ;
    		}
    	},
    	notspecified {
    		//License Not Specified
    		public String toString() {
    			return "cc-nc" ;
    		}
    	},
    	odcby {
    		//Open Data Commons Attribution License
    		//http://opendefinition.org/licenses/odc-by
    		public String toString() {
    			return "odc-by" ;
    		}
    	},
    	odcodbl {
    		//Open Data Commons Open Database License (ODbL)
    		//http://www.opendefinition.org/licenses/odc-odbl
    		public String toString() {
    			return "odc-odbl" ;
    		}
    	},
    	otherat {
    		//Other (Attribution)
    		public String toString() {
    			return "other-at" ;
    		}
    	},
    	othernc {
    		//Other (Non-Commercial)
    		public String toString() {
    			return "other-nc" ;
    		}
    	},
    	otherclosed {
    		//Other (Not Open)
    		public String toString() {
    			return "other-closed" ;
    		}
    	},
    	otheropen {
    		//Other (Open)
    		public String toString() {
    			return "other-open" ;
    		}
    	},
    	otherpd {
    		//Other (Public Domain)
    		public String toString() {
    			return "other-pd" ;
    		}
    	},
    	ukogl {
    		//UK Open Government Licence (OGL)
    		//http://reference.data.gov.uk/id/open-government-licence
    		public String toString() {
    			return "uk-ogl" ;
    		}
    	}
    }
    
    private String apiUri = "http://ckan.opendata.cz/api/3/action";
    
    private String apiKey = "";
    
    private boolean loadToCKAN = true;
    
    @EntityDescription.Property(uri = "http://linked.opendata.cz/ontology/ckan/datasetID")
    private String datasetID = "";
    
    private String filename = "ckan-api.json";
    
    private String orgID = "d2664e4e-25ba-4dcc-a842-dcc5f2d2f326" ;
    
	private String loadLanguage = "cs";
	
    @EntityDescription.Property(uri = "http://linked.opendata.cz/ontology/ckan/generateVirtuosoTurtleExampleResource")
	private boolean generateVirtuosoTurtleExampleResource = true ;
    
    @EntityDescription.Property(uri = "http://linked.opendata.cz/ontology/ckan/generateExampleResource")
	private boolean generateExampleResource = true ;

    @EntityDescription.Property(uri = "http://linked.opendata.cz/ontology/ckan/overwrite")
    private boolean overwrite = false ;
    
    public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getDatasetID() {
		return datasetID;
	}

	public void setDatasetID(String datasetID) {
		this.datasetID = datasetID;
	}

	public String getOrgID() {
		return orgID;
	}

	public void setOrgID(String orgID) {
		this.orgID = orgID;
	}

	public String getApiUri() {
		return apiUri;
	}

	public void setApiUri(String apiUri) {
		this.apiUri = apiUri;
	}

	public boolean isLoadToCKAN() {
		return loadToCKAN;
	}

	public void setLoadToCKAN(boolean loadToCKAN) {
		this.loadToCKAN = loadToCKAN;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getLoadLanguage() {
		return loadLanguage;
	}

	public void setLoadLanguage(String loadLanguage) {
		this.loadLanguage = loadLanguage;
	}

	public boolean isGenerateVirtuosoTurtleExampleResource() {
		return generateVirtuosoTurtleExampleResource;
	}

	public void setGenerateVirtuosoTurtleExampleResource(
			boolean generateVirtuosoTurtleExampleResource) {
		this.generateVirtuosoTurtleExampleResource = generateVirtuosoTurtleExampleResource;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public boolean isGenerateExampleResource() {
		return generateExampleResource;
	}

	public void setGenerateExampleResource(boolean generateExampleResource) {
		this.generateExampleResource = generateExampleResource;
	}

}
