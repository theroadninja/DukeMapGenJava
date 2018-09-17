package trn.prefab;

class ConnectorTypeFilter implements ConnectorFilter {
	
	final int spriteLotag;
	
	public ConnectorTypeFilter(int spriteLotag){
		this.spriteLotag = spriteLotag;
	}

	@Override
	public boolean matches(Connector c) {
		return c.getConnectorType() == this.spriteLotag;
	}
	
	@Override
	public String toString(){
		return "{ ConnectorFilter spriteLotag=" + this.spriteLotag + "}";
	}

}
