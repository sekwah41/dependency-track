/*
 * This file is part of Dependency-Track.
 *
 * Dependency-Track is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Dependency-Track is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Dependency-Track. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) Axway. All Rights Reserved.
 */

package org.owasp.dependencytrack.dao;

import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.owasp.dependencytrack.model.Roles;
import org.owasp.dependencytrack.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserDao {

    /**
     * The Hibernate SessionFactory
     */
    @Autowired
    private SessionFactory sessionFactory;


    public void registerUser(String username, String password, Integer role) {
        final RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        final Object salt = rng.nextBytes();
        // todo: need to change this.
        final String hashedPasswordBase64 = new Sha256Hash(password, salt.toString()).toBase64();
        Query query;
        if (role == null) {
            query = sessionFactory.getCurrentSession().createQuery("FROM Roles as r where r.role  =:role");
            query.setParameter("role", "user");
        } else {
            query = sessionFactory.getCurrentSession().createQuery("FROM Roles as r where r.id  =:role");
            query.setParameter("role", role);
        }


        final User user = new User();
        user.setPassword(hashedPasswordBase64);
        user.setUsername(username);
        user.setCheckvalid(false);
        user.setRoles((Roles) query.list().get(0));
        user.setPasswordSalt(salt.toString());
        sessionFactory.getCurrentSession().save(user);
    }

    public String hashpwd(String username, String password) {
        final Query query = sessionFactory.getCurrentSession().createQuery("FROM User where username =:usrn");
        query.setParameter("usrn", username);
        if (query.list().isEmpty()) {
            return null;
        }
        final User user = (User) query.list().get(0);
        return new Sha256Hash(password, user.getPasswordSalt()).toBase64();
    }

    @SuppressWarnings("unchecked")
    public List<User> accountManagement() {
        final Query query = sessionFactory.getCurrentSession().createQuery("FROM User ");
        return query.list();
    }

    public void validateuser(int userid) {
        Query query = sessionFactory.getCurrentSession().createQuery("select usr.checkvalid FROM User as usr where usr.id= :userid");
        query.setParameter("userid", userid);

        final Boolean currentState = (Boolean) query.list().get(0);

        if (currentState) {
            query = sessionFactory.getCurrentSession().createQuery("update User as usr set usr.checkvalid  = :checkinvalid" +
                    " where usr.id = :userid");
            query.setParameter("checkinvalid", false);
            query.setParameter("userid", userid);
            query.executeUpdate();
        } else {
            query = sessionFactory.getCurrentSession().createQuery("update User as usr set usr.checkvalid  = :checkvalid" +
                    " where usr.id = :userid");
            query.setParameter("checkvalid", true);
            query.setParameter("userid", userid);
            query.executeUpdate();
        }
    }

    public void deleteUser(int userid) {
        final Session session = sessionFactory.openSession();
        session.beginTransaction();

        final Query query = sessionFactory.getCurrentSession().createQuery(" FROM User as usr where usr.id= :userid");
        query.setParameter("userid", userid);

        final User curUser = (User) query.list().get(0);

        session.delete(curUser);
        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    public List<Roles> getRoleList() {
        final Session session = sessionFactory.openSession();
        session.beginTransaction();

        final Query query = sessionFactory.getCurrentSession().createQuery(" FROM Roles  ");

        final ArrayList<Roles> rolelist = (ArrayList<Roles>) query.list();
        session.close();
        return rolelist;
    }

    public void changeUserRole(int userid, int role) {
        final Session session = sessionFactory.openSession();
        session.beginTransaction();

        final Query query = sessionFactory.getCurrentSession().createQuery("update User as usr set usr.roles.id  = :role" +
                " where usr.id = :userid");
        query.setParameter("role", role);
        query.setParameter("userid", userid);
        query.executeUpdate();

        session.getTransaction().commit();
        session.close();
    }
}
