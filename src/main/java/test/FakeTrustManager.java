/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.

 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package test;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;


public class FakeTrustManager implements X509TrustManager
{
  public FakeTrustManager()
  {
  }


  @Override
  public void checkClientTrusted(X509Certificate[] x509Certificate, String name) throws CertificateException
  {
  }


  @Override
  public void checkServerTrusted(X509Certificate[] certificates, String name) throws CertificateException
  {
  }


  @Override
  public X509Certificate[] getAcceptedIssuers()
  {
    return(null);
  }
}