package org.ouyalibre.tv;
import java.util.List;
public interface PagedSource extends Source {
 List<Item> search(String query,int page)throws Exception;
}
