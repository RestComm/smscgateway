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

/**
 * SSL Configuration to be used with {@link SmppServerManagement} and
 * {@link Esme}
 * 
 * @author Amit Bhayani
 * 
 */
public interface SslConfigurationWrapperMBean {
	
	/**
	 * Set to true if SSL is needed
	 * 
	 * @param value
	 */
	public void setUseSsl(boolean value);
	
	/**
	 * Returns true if SSL is used
	 * 
	 * @return
	 */
	public boolean isUseSsl();

	/**
	 * @return Alias of SSL certificate for the connector
	 */
	public String getCertAlias();

	/**
	 * @return Path to file that contains Certificate Revocation List
	 */
	public String getCrlPath();

	/**
	 * @return The algorithm name (default "SunX509") used by the
	 *         {@link KeyManagerFactory}
	 */
	public String getKeyManagerFactoryAlgorithm();

	/**
	 * @return The password (if any) for the specific key within the key store
	 */
	public String getKeyManagerPassword();

	/**
	 * Get the Key Store Password
	 * 
	 * @return
	 */
	public String getKeyStorePassword();
	
    /**
     * @return The file or URL of the SSL Key store.
     */
    public String getKeyStorePath();

	/**
	 * Get the path for Key Store
	 * 
	 * @return
	 */
	public String getKeyStoreProvider();
	
    /**
     * @param keyStoreProvider The provider of the key store
     */
    public void setKeyStoreProvider(String keyStoreProvider);

	/**
	 * @return The type of the key store (default "JKS")
	 */
	public String getKeyStoreType();

	/**
	 * @return Maximum number of intermediate certificates in the certification
	 *         path (-1 for unlimited)
	 */
	public int getMaxCertPathLength();

	/**
	 * @return True if SSL needs client authentication.
	 * @see SSLEngine#getNeedClientAuth()
	 */
	public boolean getNeedClientAuth();

	/**
	 * @return Location of the OCSP Responder
	 */
	public String getOcspResponderURL();

	/**
	 * @return The SSL protocol (default "TLS") passed to
	 *         {@link SSLContext#getInstance(String, String)}
	 */
	public String getProtocol();

	/**
	 * @return The SSL provider name, which if set is passed to
	 *         {@link SSLContext#getInstance(String, String)}
	 */
	public String getProvider();

	/**
	 * @return The algorithm name, which if set is passed to
	 *         {@link SecureRandom#getInstance(String)} to obtain the
	 *         {@link SecureRandom} instance passed to
	 *         {@link SSLContext#init(javax.net.ssl.KeyManager[], javax.net.ssl.TrustManager[], SecureRandom)}
	 */
	public String getSecureRandomAlgorithm();

	/**
	 * Get SSL session cache size.
	 * 
	 * @return SSL session cache size
	 */
	public int getSslSessionCacheSize();

	/**
	 * Get SSL session timeout.
	 * 
	 * @return SSL session timeout
	 */
	public int getSslSessionTimeout();

	/**
	 * @return The algorithm name (default "SunX509") used by the
	 *         {@link TrustManagerFactory}
	 */
	public String getTrustManagerFactoryAlgorithm();

	/**
	 * @return The password for the trust store
	 */
	public String getTrustStorePassword();

	/**
	 * @return The file name or URL of the trust store location
	 */
	public String getTrustStorePath();

	/**
	 * @return The provider of the trust store
	 */
	public String getTrustStoreProvider();

	/**
	 * @return The type of the trust store (default "JKS")
	 */
	public String getTrustStoreType();

	/**
	 * @return True if SSL wants client authentication.
	 * @see SSLEngine#getWantClientAuth()
	 */
	public boolean getWantClientAuth();

	/**
	 * @return True if SSL re-negotiation is allowed (default false)
	 */
	public boolean isAllowRenegotiate();

	/**
	 * @return true if CRL Distribution Points support is enabled
	 */
	public boolean isEnableCRLDP();

	/**
	 * @return true if On-Line Certificate Status Protocol support is enabled
	 */
	public boolean isEnableOCSP();

	/**
	 * @return true if SSL Session caching is enabled
	 */
	public boolean isSessionCachingEnabled();

	/**
	 * @return True if all certificates should be trusted if there is no
	 *         KeyStore or TrustStore
	 */
	public boolean isTrustAll();

	/**
	 * @return true if SSL certificate has to be validated
	 */
	public boolean isValidateCerts();

	/**
	 * @return true if SSL certificates of the peer have to be validated
	 */
	public boolean isValidatePeerCerts();

	/**
	 * Set if SSL re-negotiation is allowed. CVE-2009-3555 discovered a
	 * vulnerability in SSL/TLS with re-negotiation. If your JVM does not have
	 * CVE-2009-3555 fixed, then re-negotiation should not be allowed.
	 * CVE-2009-3555 was fixed in Sun java 1.6 with a ban of renegotiates in u19
	 * and with RFC5746 in u22.
	 * 
	 * @param allowRenegotiate
	 *            true if re-negotiation is allowed (default false)
	 */
	public void setAllowRenegotiate(boolean allowRenegotiate);

	/**
	 * @param certAlias
	 *            Alias of SSL certificate for the connector
	 */
	public void setCertAlias(String certAlias);

	/**
	 * @param crlPath
	 *            Path to file that contains Certificate Revocation List
	 */
	public void setCrlPath(String crlPath);

	/**
	 * Enables CRL Distribution Points Support
	 * 
	 * @param enableCRLDP
	 *            true - turn on, false - turns off
	 */
	public void setEnableCRLDP(boolean enableCRLDP);

	/**
	 * Enables On-Line Certificate Status Protocol support
	 * 
	 * @param enableOCSP
	 *            true - turn on, false - turn off
	 */
	public void setEnableOCSP(boolean enableOCSP);

	/**
	 * @param algorithm
	 *            The algorithm name (default "SunX509") used by the
	 *            {@link KeyManagerFactory}
	 */
	public void setKeyManagerFactoryAlgorithm(String algorithm);

	/**
	 * 
	 * @param password
	 *            The password (if any) for the specific key within the key
	 *            store
	 */
	public void setKeyManagerPassword(String password);

	/**
	 * Set Key Store Password
	 * 
	 * @param password
	 */
	public void setKeyStorePassword(String password);

	/**
	 * Set the absolute path for key store
	 * 
	 * @param keyStorePath
	 */
	public void setKeyStorePath(String keyStorePath);

	/**
	 * @param keyStoreType
	 *            The type of the key store (default "JKS")
	 */
	public void setKeyStoreType(String keyStoreType);

	/**
	 * @param maxCertPathLength
	 *            maximum number of intermediate certificates in the
	 *            certification path (-1 for unlimited)
	 */
	public void setMaxCertPathLength(int maxCertPathLength);

	/**
	 * @param needClientAuth
	 *            True if SSL needs client authentication.
	 */
	public void setNeedClientAuth(boolean needClientAuth);

	/**
	 * Set the location of the OCSP Responder.
	 * 
	 * @param ocspResponderURL
	 *            location of the OCSP Responder
	 */
	public void setOcspResponderURL(String ocspResponderURL);

	/**
	 * @param protocol
	 *            The SSL protocol (default "TLS") passed to
	 *            {@link SSLContext#getInstance(String, String)}
	 */
	public void setProtocol(String protocol);

	/**
	 * @param provider
	 *            The SSL provider name, which if set is passed to
	 *            {@link SSLContext#getInstance(String, String)}
	 */
	public void setProvider(String provider);

	/**
	 * @param algorithm
	 *            The algorithm name, which if set is passed to
	 *            {@link SecureRandom#getInstance(String)} to obtain the
	 *            {@link SecureRandom} instance passed to
	 *            {@link SSLContext#init(javax.net.ssl.KeyManager[], javax.net.ssl.TrustManager[], SecureRandom)}
	 */
	public void setSecureRandomAlgorithm(String algorithm);

	/**
	 * Set the flag to enable SSL Session caching.
	 * 
	 * @param enableSessionCaching
	 *            the value of the flag
	 */
	public void setSessionCachingEnabled(boolean enableSessionCaching);

	/**
	 * Set SSL session cache size.
	 * 
	 * @param sslSessionCacheSize
	 *            SSL session cache size to set
	 */
	public void setSslSessionCacheSize(int sslSessionCacheSize);

	/**
	 * Set SSL session timeout.
	 * 
	 * @param sslSessionTimeout
	 *            SSL session timeout to set
	 */
	public void setSslSessionTimeout(int sslSessionTimeout);

	/**
	 * @param trustAll
	 *            True if all certificates should be trusted if there is no
	 *            KeyStore or TrustStore
	 */
	public void setTrustAll(boolean trustAll);

	/**
	 * @param algorithm
	 *            The algorithm name (default "SunX509") used by the
	 *            {@link TrustManagerFactory} Use the string "TrustAll" to
	 *            install a trust manager that trusts all.
	 */
	public void setTrustManagerFactoryAlgorithm(String algorithm);

	/**
	 * @param password
	 *            The password for the trust store
	 */
	public void setTrustStorePassword(String password);

	/**
	 * @param trustStorePath
	 *            The file name or URL of the trust store location
	 */
	public void setTrustStorePath(String trustStorePath);

	/**
	 * @param trustStoreProvider
	 *            The provider of the trust store
	 */
	public void setTrustStoreProvider(String trustStoreProvider);

	/**
	 * @param trustStoreType
	 *            The type of the trust store (default "JKS")
	 */
	public void setTrustStoreType(String trustStoreType);

	/**
	 * @param validateCerts
	 *            true if SSL certificates have to be validated
	 */
	public void setValidateCerts(boolean validateCerts);

	/**
	 * @param validatePeerCerts
	 *            true if SSL certificates of the peer have to be validated
	 */
	public void setValidatePeerCerts(boolean validatePeerCerts);

	/**
	 * @param wantClientAuth
	 *            True if SSL wants client authentication.
	 */
	public void setWantClientAuth(boolean wantClientAuth);

}
