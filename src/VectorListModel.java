import java.util.Vector;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

public class VectorListModel<String> extends Vector<String> implements ListModel<String>{

	public VectorListModel(Vector<String> list){
		super(list);
	}
	public VectorListModel() {
		super();
	}
	@Override
	public void addListDataListener(ListDataListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getElementAt(int index) {
		return this.get(index);
	}

	@Override
	public int getSize() {
		return this.size();
	}

	@Override
	public void removeListDataListener(ListDataListener arg0) {
		// TODO Auto-generated method stub
		
	}

}
