package database.js.database;

public class BindValue
{
  private final boolean out;
  private final BindValueDef bindvalue;

  public BindValue(BindValueDef bindvalue, boolean out)
  {
    this.out = out;
    this.bindvalue = bindvalue;
  }

  public boolean InOut()
  {
    return(out);
  }

  public String getName()
  {
    return(bindvalue.name);
  }

  public int getType()
  {
    return(bindvalue.type);
  }

  public Object getValue()
  {
    return(bindvalue.value);
  }
}
