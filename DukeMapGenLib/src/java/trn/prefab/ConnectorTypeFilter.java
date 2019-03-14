package trn.prefab;

class ConnectorTypeFilter implements ConnectorFilter {
	
	final int spriteLotag;
	
	public ConnectorTypeFilter(int spriteLotag){
		this.spriteLotag = spriteLotag;
	}

	@Override
	public boolean matches(Connector c) {
		if(c instanceof RedwallConnector){ // TODO - gross
			return ((RedwallConnector)c).getConnectorType() == this.spriteLotag;
		} else {
			//throw new RuntimeException("c not isntance of " + c.getClass().getName().toString());
			return false;
		}
	}
	
	@Override
	public String toString(){
		return "{ ConnectorFilter spriteLotag=" + this.spriteLotag + "}";
	}

}
