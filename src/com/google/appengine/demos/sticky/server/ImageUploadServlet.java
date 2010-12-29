package com.google.appengine.demos.sticky.server;

import gwtupload.server.exceptions.UploadActionException;
import gwtupload.server.gae.AppEngineUploadAction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.demos.sticky.server.Store.Author;
import com.google.appengine.demos.sticky.server.Store.Note;

@SuppressWarnings("serial")
public class ImageUploadServlet extends AppEngineUploadAction {

    private static String NOTE_KEY = "noteKey";

    @Override
    public String executeAction(HttpServletRequest request, List<FileItem> sessionFiles) throws UploadActionException {

        String response = "";
        String contentType = "";
        String noteKey = "";
        byte[] bytes = null;

        if (sessionFiles != null) {
            for (FileItem fItem : sessionFiles) {
                if (fItem.isFormField() == false) {

                    contentType = fItem.getContentType();

                    InputStream stream;
                    try {
                        int bufSize = 8192;
                        int size = (int) fItem.getSize();
                        if (bufSize > size) {
                            bufSize = size;
                        }
                        int bytesRead = 0;

                        byte[] buffer = new byte[bufSize];
                        stream = fItem.getInputStream();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        int length = stream.read(buffer, 0, buffer.length);
                        bytesRead += length;
                        while (length != -1 && bytesRead <= size) {
                            bos.write(buffer, 0, length);
                            length = stream.read(buffer, 0, buffer.length);
                            bytesRead += length;
                        }
                        bytes = bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (fItem.getFieldName().equals(NOTE_KEY)) {
                    try {
                        noteKey = fItem.getString("UTF-8").trim();
                    } catch (UnsupportedEncodingException ex) {
                        throw new UploadActionException("Error decoding Note-Key!");
                    }
                }
            }

            if (!persistNoteImage(contentType, bytes, noteKey)) {
                throw new UploadActionException("Error persisting image!");
            }

        }

        removeSessionFileItems(request);
        return (response.length() == 0) ? null : response;
    }

    private boolean persistNoteImage(String contentType, byte[] bytes, String noteKey) {
        Store.Api api = ServiceImpl.store.getApi();

        User user = UserServiceFactory.getUserService().getCurrentUser();
        // NoteImage image = new NoteImage(bytes, contentType, user.getEmail(),
        // new Date(), new Date());

        try {
            Author me = api.getOrCreateNewAuthor(user);

            Transaction tx = api.begin();
            Note note = api.getNote(KeyFactory.stringToKey(noteKey));
            if (!note.getAuthorEmail().equalsIgnoreCase(me.getEmail())) {
                return false;
            }

            note.setImageData(new Blob(bytes));
            note.setContentType(contentType);
            api.saveNote(note);
            tx.commit();

        } catch (Exception ex) {
            return false;
        } finally {
            api.close();
        }
        return true;
    }

}
