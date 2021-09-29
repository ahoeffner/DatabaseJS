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
