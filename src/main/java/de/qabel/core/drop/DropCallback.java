package de.qabel.core.drop;

public interface DropCallback<T extends ModelObject> {
	void onDropMessage(DropMessage<T> message);
}