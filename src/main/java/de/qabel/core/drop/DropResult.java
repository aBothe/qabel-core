package de.qabel.core.drop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class DropResult: Save the return value of every contact and the overview
 * whether everything is okay
 */
public class DropResult {
	private boolean checked = false;
	private boolean success = true;
	private List<DropResultContact> results;

	public DropResult() {
		this.results = new ArrayList<DropResultContact>();
	}

	/**
	 * Constructor
	 * 
	 * @param results
	 *            List of all results
	 */
	public DropResult(List<DropResultContact> results) {
		Iterator<DropResultContact> iterator;
		
		this.results = results;
		
		iterator = this.results.iterator();
		while (iterator.hasNext()) {
			DropResultContact item;
			
			this.checked = true;
			
			item = iterator.next();
			if (item.isSuccess() == false) {
				this.success = false;
			}
		}
	}

	/**
	 * Add one result
	 * 
	 * @param result Object of DropResultContact
	 */
	public void addContactResult(DropResultContact result) {
		this.results.add(result);
		
		this.checked = true;
		
		if (result.isSuccess() == false) {
			this.success = false;
		}
	}

	/**
	 * Return the overview state of the complete shipping
	 * 
	 * @return State of the shipping
	 */
	public boolean isSuccess() {
		if (this.checked == false) {
			return false;
		}

		return this.success;
	}

	/**
	 * Return the list of all results of the contacts
	 * 
	 * @return List of all results
	 */
	public List<DropResultContact> getList() {
		return Collections.unmodifiableList(this.results);
	}

}
