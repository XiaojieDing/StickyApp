package com.google.appengine.demos.sticky.server;

import java.io.IOException;

import javax.jdo.Transaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.demos.sticky.server.Store.Note;

@SuppressWarnings("serial")
public class ImageFetchServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String list[] = uri.split("/");
        // remove special attachment to reload image
        String key = "";
        if (uri.contains("=reload=")) {
            key = list[list.length - 1].substring(0, list[list.length - 1].indexOf("=reload="));
        } else {
            key = list[list.length - 1];
        }
        Note note = getNoteFromStore(key);

        if (note != null) {
            response.setContentType(note.getContentType());
            response.getOutputStream().write(note.getImageData().getBytes());
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private Note getNoteFromStore(String strKey) {
        Store.Api api = ServiceImpl.store.getApi();
        Note note = null;

        try {
            Key key = KeyFactory.stringToKey(strKey);
            Transaction tx = api.begin();
            note = api.getNote(key);
            tx.commit();

        } catch (Exception ex) {
        } finally {
            api.close();
        }

        return note;
    }

}
