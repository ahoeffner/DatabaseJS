package config;

public class Keystore
{
  public final String file;
  public final String type;
  public final String alias;
  public final String password;


  public Keystore(String file, String type, String alias, String password)
  {
    this.file = file;
    this.type = type;
    this.alias = alias;
    this.password = password;
  }

  @Override
  public String toString()
  {
    return(file+" "+type+" "+(alias == null ? "" : alias));
  }
}
