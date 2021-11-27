package trn.prefab;

@Deprecated
public interface ConnectorFilter {
	boolean matches(Connector c);
	
	static boolean allMatch(final Connector c, ConnectorFilter ... filters){
		for(ConnectorFilter f : filters){
			if(! f.matches(c)){
				return false;
			}
		}
		return true;
	}
}