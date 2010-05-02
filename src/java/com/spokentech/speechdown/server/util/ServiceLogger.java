package com.spokentech.speechdown.server.util;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.spokentech.speechdown.server.domain.HttpRequest;
import com.spokentech.speechdown.server.domain.RecogRequest;
import com.spokentech.speechdown.server.domain.SynthRequest;


public class ServiceLogger {

	public static void logHttpRequest(HttpRequest hr) throws Exception {
		
	     Transaction transaction = null;
	     Session session = null;
	     try {
	         session = HibernateUtil.getSessionFactory().getCurrentSession();
	         transaction = session.beginTransaction();
	         session.save(hr);
	         session.getTransaction().commit();
	      } catch (Exception e) { 
	           if (transaction != null) {
	             transaction.rollback();
	             throw e;
	           }
	      }  finally { 
	           session.close();
	      }
    }
	
	
	
  
	public static void logRecogRequest(RecogRequest rr,HttpRequest hr) throws Exception {
		
	     Transaction transaction = null;
	     Session session = null;
	     try {
	         session = HibernateUtil.getSessionFactory().getCurrentSession();
	         transaction = session.beginTransaction();
	         session.save(rr);
	         session.getTransaction().commit();
	      } catch (Exception e) { 
	           if (transaction != null) {
	             transaction.rollback();
	             throw e;
	           }
	      }  finally { 
	           session.close();
	      }	
    }
	
	public static void logSynthRequest(SynthRequest sr,HttpRequest hr) throws Exception {
		
	     Transaction transaction = null;
	     Session session = null;
	     try {
	         session = HibernateUtil.getSessionFactory().getCurrentSession();
	         transaction = session.beginTransaction();
	         session.save(sr);
	         session.getTransaction().commit();
	      } catch (Exception e) { 
	           if (transaction != null) {
	             transaction.rollback();
	             throw e;
	           }
	      }  finally { 
	           session.close();
	      }
    }

	
	
	public static void printHttpLog() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List result = session.createQuery("from HttpRequest").list();
        session.getTransaction().commit();


	    for (int i = 0; i < result.size(); i++) {
	    	HttpRequest item = (HttpRequest) result.get(i);
	        System.out.println(
	                "HTTP Log: " + item.getMethod() + item.getScheme() + item.getContextPath() +" Time: " + item.getDate()
	        );
	    }
	}


	public static void printRecogLog() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List result = session.createQuery("from RecogRequest").list();
        session.getTransaction().commit();


	    for (int i = 0; i < result.size(); i++) {
	    	RecogRequest item = (RecogRequest) result.get(i);
	        System.out.println(
	                "Recog Log: " + item.getRawResults() + item.getPronunciation() + item.getStreamLen() +" Time: " + item.getDate()
	        );
	    }
	}
    
    
}
