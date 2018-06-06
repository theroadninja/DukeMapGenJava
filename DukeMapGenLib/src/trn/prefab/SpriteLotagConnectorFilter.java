package trn.prefab;

class SpriteLotagConnectorFilter implements ConnectorFilter {
	
	final int spriteLotag;
	
	public SpriteLotagConnectorFilter(int spriteLotag){
		this.spriteLotag = spriteLotag;
	}

	@Override
	public boolean matches(Connector c) {
		return c.getMarkerSpriteLotag() == this.spriteLotag;
	}
	
	@Override
	public String toString(){
		return "{ ConnectorFilter spriteLotag=" + this.spriteLotag + "}";
	}

}
