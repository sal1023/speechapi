/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.common.rule;


public class RuleMatch {

    private String _rule;
    private String _tag;
    
    
    public RuleMatch() {
    	_rule=null;
    	_tag=null;
    }

    public RuleMatch(String rule, String tag) {
        _rule = rule;
        _tag = tag;
    }

    public String getRule() {
        return _rule;
    }

    public void setRule(String rule) {
        _rule = rule;
    }

    public String getTag() {
        return _tag;
    }

    public void setTag(String tag) {
        _tag = tag;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RuleMatch) {
            if ((_rule.equals(((RuleMatch) obj).getRule()))  && 
                ( _tag.equals(((RuleMatch) obj).getTag())) ) {
               return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return _tag.concat(_rule).hashCode();
    }

    
}
