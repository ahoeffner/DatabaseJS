package database.js.pools;

public interface PoolResponse
{
  void failed();
  void respond(byte[] data) throws Exception;
}
