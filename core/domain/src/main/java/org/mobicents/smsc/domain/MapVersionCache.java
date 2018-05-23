/**
 * 
 */
package org.mobicents.smsc.domain;

import javolution.util.FastMap;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion;

/**
 * @author Amit Bhayani
 * 
 */
public class MapVersionCache implements MapVersionCacheMBean {

	private FastMap<String, MapVersionNeg> cache = new FastMap<String, MapVersionNeg>()
			.shared();

	private final String name;

	private static MapVersionCache instance;

	/**
	 * 
	 */
	private MapVersionCache(String name) {
		this.name = name;
	}

	public static MapVersionCache getInstance(String name) {
		if (instance == null) {
			instance = new MapVersionCache(name);
		}
		return instance;
	}

	public static MapVersionCache getInstance() {
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.MapVersionCacheMBean#getMAPApplicationContextVersion
	 * (java.lang.String)
	 */
	@Override
	public MAPApplicationContextVersion getMAPApplicationContextVersion(String globalTitleDigits) {
        MapVersionNeg neg = this.cache.get(globalTitleDigits);
        if (neg != null)
            return neg.getCurVersion();
        else
            return null;
	}

    public void setMAPApplicationContextVersion(String globalTitleDigits, MAPApplicationContextVersion version) {
        MapVersionNeg neg = this.cache.get(globalTitleDigits);
        if (neg != null)
            neg.registerCheckedVersion(version);
        else {
            neg = new MapVersionNeg(globalTitleDigits);
            this.cache.put(globalTitleDigits, neg);
            neg.registerCheckedVersion(version);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mobicents.smsc.smpp.MapVersionCacheMBean#setMAPApplicationContextVersion
     * (java.lang.String,
     * org.restcomm.protocols.ss7.map.api.MAPApplicationContextVersion)
     */
    @Override
    public void forceMAPApplicationContextVersion(String globalTitleDigits, MAPApplicationContextVersion version) {
        MapVersionNeg neg = new MapVersionNeg(globalTitleDigits);
        this.cache.put(globalTitleDigits, neg);
        neg.registerCheckedVersion(version);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.MapVersionCacheMBean#
	 * getMAPApplicationContextVersionCache()
	 */
	@Override
	public FastMap<String, MapVersionNeg> getMAPApplicationContextVersionCache() {
		return this.cache;
	}

	@Override
	public void forceClear() {
		this.cache.clear();
	}
}
