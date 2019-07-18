/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cm.projects.spring.resource.chasis.utils;

import java.util.Map;

/**
 *Used create a single Key Value Pair
 * @author Cornelius M
 * @param <K>
 * @param <V>
 */
public class CustomEntry<K extends Object, V extends Object> implements Map.Entry<K, V> {
    private K k;
    private  V v;
    public CustomEntry(K k, V v){
        this.k = k;
        this.v = v;
    }
    public CustomEntry(){
        
    }
    /**
     * Used to set key and value. Used especially when using the default constructor
     * @param k
     * @param v 
     */
    public void set(K k, V v){
        this.k = k;
        this.v = v;
    }
    @Override
    public K getKey() {
        return k;
    }

    @Override
    public V getValue() {
        return v;
    }

    @Override
    public V setValue(V v) {
        this.v = v;
        return this.v;
    }    
    public boolean hasKey(){
        return k != null;
    }
    public boolean hasKeyAndValue(){
        return (k != null) && ( v != null);
    }
    
    @Override
    public String toString(){
        return "Key: "+k+", Value: "+v;
    }
}