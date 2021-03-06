/*
 * The MIT License
 *
 * Copyright 2020 Kaushal.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.example.library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 *
 * @author Kaushal
 */
public class Operation extends javax.swing.JFrame {

    String s_id;
    String action;
    String operation;

    ResultSet r = null;
    ResultSet s = null;
    PreparedStatement p = null;
    PreparedStatement q = null;
    Connection c = null;

    /**
     * Creates new form Operation
     *
     * @param params the command line arguments
     */
    public Operation(String params[]) {
        s_id = params[0];
        action = params[2];
        operation = params[3];
        initComponents();
        initForm();
        if (params[1].length() > 0) {
            jTextField2.setText(params[1]);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel1.setText("User-Id");

        jTextField1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        jButton1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jButton1.setText("Delete");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jButton2.setText("Cancel");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextField2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel2.setText("Book-Id");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 169, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField2)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void initForm() {
        if (action.equals("type-b") && !(operation.equals("Issue") || operation.equals("Return"))) {
            jLabel1.setVisible(false);
            jTextField1.setVisible(false);
        }
        if (action.equals("type-u") && !(operation.equals("Issue") || operation.equals("Return"))) {
            jLabel2.setVisible(false);
            jTextField2.setVisible(false);
        }
        jButton1.setText(operation);
    }
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        String u_id = jTextField1.getText();
        String b_id = jTextField2.getText();
        ArrayList<String> messages = new ArrayList<>();

        if (s_id.equals(u_id) && action.equals("type-u")) {
            messages.add("Can't delete your own user account.");
        }
        if (isIdValid(u_id) == false && jTextField1.isVisible()) {
            messages.add("Specified user-id is not valid.");
        }
        if (isIdValid(b_id) == false && jTextField2.isVisible()) {
            messages.add("Specified book-id is not valid.");
        }

        if (messages.size() > 0) {
            JOptionPane.showMessageDialog(null, String.join("\n", messages));
            return;
        }

        try {
            c = DriverManager.getConnection("jdbc:sqlite::resource:database/library.db");

            if (action.equals("type-u")) {
                p = c.prepareStatement("select * from user where id = ?");
                p.setString(1, u_id);
            }
            if (action.equals("type-b")) {
                p = c.prepareStatement("select * from book where id = ?");
                p.setString(1, b_id);
            }

            r = p.executeQuery();

            if (r.isBeforeFirst()) {
                if (operation.equals("Update")) {
                    Book.main(new String[]{b_id, "type-c"});
                    this.dispose();
                } else {
                    if (operation.equals("Delete")) {
                        if (action.equals("type-u")) {
                            messages.add("Do you really want to delete the user " + r.getString("name") + "?");
                        }
                        if (action.equals("type-b")) {
                            messages.add("Do you really want to delete the book " + r.getString("name") + " by " + r.getString("author") + "?");
                        }
                    }
                    if (operation.equals("Issue")) {
                        q = c.prepareStatement("select count(*) as issued from issue where book_id = ?");
                        q.setString(1, b_id);

                        s = q.executeQuery();

                        if (r.getInt("quantity") - s.getInt("issued") > 0) {
                            messages.add("Book " + r.getString("name") + " by " + r.getString("author") + " will be issued on behalf of specified user. Do you want to continue?");
                        } else {
                            JOptionPane.showMessageDialog(null, "Specified book is not available.");
                            this.dispose();
                        }
                    }
                    if (operation.equals("Return")) {
                        messages.add("Book " + r.getString("name") + " by " + r.getString("author") + " will be returned on behalf of specified user. Do you want to continue?");
                    }

                    if (JOptionPane.showConfirmDialog(null, String.join("\n", messages), "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        if (operation.equals("Delete")) {
                            if (action.equals("type-u")) {
                                p = c.prepareStatement("delete from user where id = ?");
                                p.setString(1, u_id);
                            }
                            if (action.equals("type-b")) {
                                p = c.prepareStatement("delete from book where id = ?");
                                p.setString(1, b_id);
                            }

                            if (p.executeUpdate() == 1) {
                                if (action.equals("type-u")) {
                                    messages.add("User successfully deleted.");
                                }
                                if (action.equals("type-b")) {
                                    messages.add("Book successfully deleted.");
                                }
                                JOptionPane.showMessageDialog(null, String.join("\n", messages));
                                this.dispose();
                            } else {
                                JOptionPane.showMessageDialog(null, "Error occured while deleting data.", "Failure", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        if (operation.equals("Issue")) {
                            p = c.prepareStatement("insert into issue (book_id, user_id) values (?, ?)");
                            p.setString(1, b_id);
                            p.setString(2, u_id);

                            if (p.executeUpdate() == 1) {
                                JOptionPane.showMessageDialog(null, "Book successfully issued.");
                                this.dispose();
                            } else {
                                JOptionPane.showMessageDialog(null, "Error occured while inserting data.", "Failure", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        if (operation.equals("Return")) {
                            p = c.prepareStatement("delete from issue where book_id = ? and user_id = ?");
                            p.setString(1, b_id);
                            p.setString(2, u_id);

                            if (p.executeUpdate() == 1) {
                                JOptionPane.showMessageDialog(null, "Book successfully returned.");
                                this.dispose();
                            } else {
                                JOptionPane.showMessageDialog(null, "Error occured while deleting data.", "Failure", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } else {
                        this.dispose();
                    }
                }
            } else {
                if (action.equals("type-u")) {
                    messages.add("Specified user-id is not found database.");
                }
                if (action.equals("type-b")) {
                    messages.add("Specified book-id is not found database.");
                }
                JOptionPane.showMessageDialog(null, String.join("\n", messages), "Failure", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (r != null) {
                    r.close();
                }
                if (s != null) {
                    s.close();
                }
                if (p != null) {
                    p.close();
                }
                if (q != null) {
                    q.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed

    /**
     * Checks validity of id
     *
     * @param id
     *
     * @return Boolean value showing if id is valid or not
     */
    public static boolean isIdValid(String id) {
        Pattern pat = Pattern.compile("\\d+");
        if (id == null) {
            return false;
        }
        return pat.matcher(id).matches();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Operation.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new Operation(args).setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
