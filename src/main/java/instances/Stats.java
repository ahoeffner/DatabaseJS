package instances;

import java.io.Serializable;


public class Stats implements Serializable
{
  @SuppressWarnings("compatibility:-2705344686479478538")
  private static final long serialVersionUID = 5741949964475085825L;

  public final int ses;
  public final int max;
  public final long mem;
  public final long used;
  
  
  public Stats(int ses, int max)
  {
    this.ses = ses;
    this.max = max;
    
    this.mem = Runtime.getRuntime().totalMemory();
    this.used = (mem - Runtime.getRuntime().freeMemory());
  }
}
