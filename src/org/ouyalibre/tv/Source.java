package org.ouyalibre.tv;
import java.util.List;
public interface Source {
 String name();
 List<Item> search(String query)throws Exception;
 List<Stream> resolve(Item item)throws Exception;
}
