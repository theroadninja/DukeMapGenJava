package trn.prefab;

public interface ConnectorFilter {
	public boolean matches(Connector c);
	
	public static boolean allMatch(final Connector c, ConnectorFilter ... filters){
		for(ConnectorFilter f : filters){
			if(! f.matches(c)){
				return false;
			}
		}
		return true;
	}
}