package be.ac.vub.platformkit.servlet;


public class ConstraintSpacePool {
private java.util.Set space = new java.util.HashSet();

private java.util.Date lastModified = null;

public java.util.Date getLastModified() {
return lastModified;
}

public void setLastModified(java.util.Date lastModified) {
this.lastModified = lastModified;
}

public java.util.Enumeration getSpaces() {
return new be.ac.vub.util.IteratorEnumerationAdapter(space.iterator());
}

public void addSpace(be.ac.vub.platformkit.ConstraintSpace space) {
if (! this.space.contains(space)) {
    this.space.add(space);
}
}

public void removeSpace(be.ac.vub.platformkit.ConstraintSpace space) {
this.space.remove(space);
}

}

