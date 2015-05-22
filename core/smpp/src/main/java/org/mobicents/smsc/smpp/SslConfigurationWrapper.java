/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.smpp;

import com.cloudhopper.smpp.ssl.SslConfiguration;

/**
 * @author Amit Bhayani
 * 
 */
public abstract class SslConfigurationWrapper implements SslConfigurationWrapperMBean {

	protected static final String USE_SSL = "useSsl";
	protected static final String CERT_ALIAS = "certAlias";
	protected static final String CRL_PATH = "crlPath";
	protected static final String KEY_MANAGER_FACTORY_ALGORITHM = "keyManagerFactoryAlgorithm";
	protected static final String KEY_MANAGER_PASSWORD = "keyManagerPassword";
	protected static final String KEY_STORE_PASSWORD = "keyStorePassword";
	protected static final String KEY_STORE_PROVIDER = "keyStoreProvider";
	protected static final String KEY_STORE_PATH = "keyStorePath";
	protected static final String KEY_STORE_TYPE = "keyStoreType";
	protected static final String MAX_CERT_PATH_LENGTH = "maxCertPathLength";
	protected static final String NEED_CLIENT_AUTH = "needClientAuth";
	protected static final String OCS_RESPONDER_URL = "ocspResponderURL";
	protected static final String PROTOCOL = "protocol";
	protected static final String PROVIDER = "provider";
	protected static final String SECURE_RANDOM_ALGORITHM = "secureRandomAlgorithm";
	protected static final String SSL_SESSION_CACHE_SIZE = "sslSessionCacheSize";
	protected static final String SSL_SESSION_TIMEOUT = "sslSessionTimeout";
	protected static final String TRUST_MANAGER_FACTORY_ALGORITHM = "trustManagerFactoryAlgorithm";
	protected static final String TRUST_STORE_PASSWORD = "trustStorePassword";
	protected static final String TRUST_STORE_PATH = "trustStorePath";
	protected static final String TRUST_STORE_PROVIDER = "trustStoreProvider";
	protected static final String TRUST_STORE_TYPE = "trustStoreType";
	protected static final String WANT_CLIENT_AUTH = "wantClientAuth";
	protected static final String ALLOW_RENEGOTIATE = "allowRenegotiate";
	protected static final String ENABLE_CRLDP = "enableCRLDP";
	protected static final String SESSION_CACHING_ENABLED = "sessionCachingEnabled";
	protected static final String TRUST_ALL = "trustAll";
	protected static final String VALIDATE_CERTS = "validateCerts";
	protected static final String VALIDATE_PEER_CERTS = "validatePeerCerts";

	protected boolean useSsl = false;

	protected SslConfiguration wrappedSslConfig;

	/**
	 * 
	 */
	public SslConfigurationWrapper() {
		this.wrappedSslConfig = new SslConfiguration();
	}

	protected SslConfiguration getWrappedSslConfig() {
		return this.wrappedSslConfig;
	}

	@Override
	public void setUseSsl(boolean value) {
		this.useSsl = value;
	}

	@Override
	public boolean isUseSsl() {
		return this.useSsl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getCertAlias()
	 */
	@Override
	public String getCertAlias() {
		return this.wrappedSslConfig.getCertAlias();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getCrlPath()
	 */
	@Override
	public String getCrlPath() {
		return this.wrappedSslConfig.getCrlPath();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#
	 * getKeyManagerFactoryAlgorithm ()
	 */
	@Override
	public String getKeyManagerFactoryAlgorithm() {
		return this.wrappedSslConfig.getKeyManagerFactoryAlgorithm();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getKeyManagerPassword
	 * ()
	 */
	@Override
	public String getKeyManagerPassword() {
		return this.wrappedSslConfig.getKeyManagerPassword();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getKeyStorePassword
	 * ()
	 */
	@Override
	public String getKeyStorePassword() {
		return this.wrappedSslConfig.getKeyStorePassword();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getKeyStoreProvider
	 * ()
	 */
	@Override
	public String getKeyStoreProvider() {
		return this.wrappedSslConfig.getKeyStoreProvider();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getKeyStoreType()
	 */
	@Override
	public String getKeyStoreType() {
		return this.wrappedSslConfig.getKeyStoreType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getMaxCertPathLength
	 * ()
	 */
	@Override
	public int getMaxCertPathLength() {
		return this.wrappedSslConfig.getMaxCertPathLength();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getNeedClientAuth()
	 */
	@Override
	public boolean getNeedClientAuth() {
		return this.wrappedSslConfig.getNeedClientAuth();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getOcspResponderURL
	 * ()
	 */
	@Override
	public String getOcspResponderURL() {
		return this.wrappedSslConfig.getOcspResponderURL();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getProtocol()
	 */
	@Override
	public String getProtocol() {
		return this.wrappedSslConfig.getProtocol();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getProvider()
	 */
	@Override
	public String getProvider() {
		return this.wrappedSslConfig.getProvider();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getSecureRandomAlgorithm
	 * ()
	 */
	@Override
	public String getSecureRandomAlgorithm() {
		return this.wrappedSslConfig.getSecureRandomAlgorithm();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getSslSessionCacheSize
	 * ()
	 */
	@Override
	public int getSslSessionCacheSize() {
		return this.wrappedSslConfig.getSslSessionCacheSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getSslSessionTimeout
	 * ()
	 */
	@Override
	public int getSslSessionTimeout() {
		return this.wrappedSslConfig.getSslSessionTimeout();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#
	 * getTrustManagerFactoryAlgorithm()
	 */
	@Override
	public String getTrustManagerFactoryAlgorithm() {
		return this.wrappedSslConfig.getTrustManagerFactoryAlgorithm();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getTrustStorePassword
	 * ()
	 */
	@Override
	public String getTrustStorePassword() {
		return this.wrappedSslConfig.getTrustStorePassword();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getTrustStorePath()
	 */
	@Override
	public String getTrustStorePath() {
		return this.wrappedSslConfig.getTrustStorePath();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getTrustStoreProvider
	 * ()
	 */
	@Override
	public String getTrustStoreProvider() {
		return this.wrappedSslConfig.getTrustStoreProvider();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getTrustStoreType()
	 */
	@Override
	public String getTrustStoreType() {
		return this.wrappedSslConfig.getTrustStoreType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#getWantClientAuth()
	 */
	@Override
	public boolean getWantClientAuth() {
		return this.wrappedSslConfig.getWantClientAuth();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#isAllowRenegotiate()
	 */
	@Override
	public boolean isAllowRenegotiate() {
		return this.wrappedSslConfig.isAllowRenegotiate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#isEnableCRLDP()
	 */
	@Override
	public boolean isEnableCRLDP() {
		return this.wrappedSslConfig.isEnableCRLDP();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#isEnableOCSP()
	 */
	@Override
	public boolean isEnableOCSP() {
		return this.wrappedSslConfig.isEnableOCSP();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#isSessionCachingEnabled
	 * ()
	 */
	@Override
	public boolean isSessionCachingEnabled() {
		return this.wrappedSslConfig.isSessionCachingEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#isTrustAll()
	 */
	@Override
	public boolean isTrustAll() {
		return this.wrappedSslConfig.isTrustAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#isValidateCerts()
	 */
	@Override
	public boolean isValidateCerts() {
		return this.wrappedSslConfig.isValidateCerts();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#isValidatePeerCerts
	 * ()
	 */
	@Override
	public boolean isValidatePeerCerts() {
		return this.wrappedSslConfig.isValidatePeerCerts();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setAllowRenegotiate
	 * (boolean )
	 */
	@Override
	public void setAllowRenegotiate(boolean allowRenegotiate) {
		this.wrappedSslConfig.setAllowRenegotiate(allowRenegotiate);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setCertAlias(java
	 * .lang .String)
	 */
	@Override
	public void setCertAlias(String certAlias) {
		this.wrappedSslConfig.setCertAlias(certAlias);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setCrlPath(java.
	 * lang. String)
	 */
	@Override
	public void setCrlPath(String crlPath) {
		this.wrappedSslConfig.setCrlPath(crlPath);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setEnableCRLDP(boolean
	 * )
	 */
	@Override
	public void setEnableCRLDP(boolean enableCRLDP) {
		this.wrappedSslConfig.setEnableCRLDP(enableCRLDP);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setEnableOCSP(boolean
	 * )
	 */
	@Override
	public void setEnableOCSP(boolean enableOCSP) {
		this.wrappedSslConfig.setEnableOCSP(enableOCSP);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#
	 * setKeyManagerFactoryAlgorithm (java.lang.String)
	 */
	@Override
	public void setKeyManagerFactoryAlgorithm(String algorithm) {
		this.wrappedSslConfig.setKeyManagerFactoryAlgorithm(algorithm);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setKeyManagerPassword
	 * (java.lang.String)
	 */
	@Override
	public void setKeyManagerPassword(String password) {
		this.wrappedSslConfig.setKeyManagerPassword(password);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setKeyStorePassword
	 * (java .lang.String)
	 */
	@Override
	public void setKeyStorePassword(String password) {
		this.wrappedSslConfig.setKeyStorePassword(password);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setKeyStorePath(
	 * java. lang.String)
	 */
	@Override
	public void setKeyStorePath(String keyStorePath) {
		this.wrappedSslConfig.setKeyStorePath(keyStorePath);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setKeyStoreType(
	 * java. lang.String)
	 */
	@Override
	public void setKeyStoreType(String keyStoreType) {
		this.wrappedSslConfig.setKeyStoreType(keyStoreType);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setMaxCertPathLength
	 * (int)
	 */
	@Override
	public void setMaxCertPathLength(int maxCertPathLength) {
		this.wrappedSslConfig.setMaxCertPathLength(maxCertPathLength);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setNeedClientAuth
	 * (boolean )
	 */
	@Override
	public void setNeedClientAuth(boolean needClientAuth) {
		this.wrappedSslConfig.setNeedClientAuth(needClientAuth);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setOcspResponderURL
	 * (java .lang.String)
	 */
	@Override
	public void setOcspResponderURL(String ocspResponderURL) {
		this.wrappedSslConfig.setOcspResponderURL(ocspResponderURL);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setProtocol(java
	 * .lang .String)
	 */
	@Override
	public void setProtocol(String protocol) {
		this.wrappedSslConfig.setProtocol(protocol);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setProvider(java
	 * .lang .String)
	 */
	@Override
	public void setProvider(String provider) {
		this.wrappedSslConfig.setProvider(provider);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setSecureRandomAlgorithm
	 * (java.lang.String)
	 */
	@Override
	public void setSecureRandomAlgorithm(String algorithm) {
		this.wrappedSslConfig.setSecureRandomAlgorithm(algorithm);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setSessionCachingEnabled
	 * (boolean)
	 */
	@Override
	public void setSessionCachingEnabled(boolean enableSessionCaching) {
		this.wrappedSslConfig.setSessionCachingEnabled(enableSessionCaching);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setSslSessionCacheSize
	 * (int)
	 */
	@Override
	public void setSslSessionCacheSize(int sslSessionCacheSize) {
		this.wrappedSslConfig.setSslSessionCacheSize(sslSessionCacheSize);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setSslSessionTimeout
	 * (int)
	 */
	@Override
	public void setSslSessionTimeout(int sslSessionTimeout) {
		this.wrappedSslConfig.setSslSessionTimeout(sslSessionTimeout);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setTrustAll(boolean)
	 */
	@Override
	public void setTrustAll(boolean trustAll) {
		this.wrappedSslConfig.setTrustAll(trustAll);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#
	 * setTrustManagerFactoryAlgorithm(java.lang.String)
	 */
	@Override
	public void setTrustManagerFactoryAlgorithm(String algorithm) {
		this.wrappedSslConfig.setTrustManagerFactoryAlgorithm(algorithm);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setTrustStorePassword
	 * (java.lang.String)
	 */
	@Override
	public void setTrustStorePassword(String password) {
		this.wrappedSslConfig.setTrustStorePassword(password);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setTrustStorePath
	 * (java .lang.String)
	 */
	@Override
	public void setTrustStorePath(String trustStorePath) {
		this.wrappedSslConfig.setTrustStorePath(trustStorePath);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setTrustStoreProvider
	 * (java.lang.String)
	 */
	@Override
	public void setTrustStoreProvider(String trustStoreProvider) {
		this.wrappedSslConfig.setTrustStoreProvider(trustStoreProvider);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setTrustStoreType
	 * (java .lang.String)
	 */
	@Override
	public void setTrustStoreType(String trustStoreType) {
		this.wrappedSslConfig.setTrustStoreType(trustStoreType);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setValidateCerts
	 * (boolean)
	 */
	@Override
	public void setValidateCerts(boolean validateCerts) {
		this.wrappedSslConfig.setValidateCerts(validateCerts);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setValidatePeerCerts
	 * ( boolean)
	 */
	@Override
	public void setValidatePeerCerts(boolean validatePeerCerts) {
		this.wrappedSslConfig.setValidatePeerCerts(validatePeerCerts);
		this.store();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.SslConfigurationWrapperMBean#setWantClientAuth
	 * (boolean )
	 */
	@Override
	public void setWantClientAuth(boolean wantClientAuth) {
		this.wrappedSslConfig.setWantClientAuth(wantClientAuth);
		this.store();
	}

	@Override
	public String getKeyStorePath() {
		return this.wrappedSslConfig.getKeyStorePath();
	}

	@Override
	public void setKeyStoreProvider(String keyStoreProvider) {
		this.wrappedSslConfig.setKeyStoreProvider(keyStoreProvider);
		this.store();
	}

	public abstract void store();

}
