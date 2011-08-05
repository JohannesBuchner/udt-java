/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package udt.util;

/**
 * Used for object pooling. 
 * @author peter
 */
public abstract class Recycler<T> {
    

    protected abstract void recycled(T r);
    
    /**
     * Accept and object for recycling.
     * @param r - the object to be recycled.
     */
    public void accept(T r){
        if ( r instanceof Recyclable){
            Recyclable rec = (Recyclable) r;
            rec.recycle();
        }
        recycled(r);
    }
    
    
    public interface Recyclable {
        public void recycle();
    }
}
