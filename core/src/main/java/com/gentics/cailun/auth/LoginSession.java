/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package com.gentics.cailun.auth;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class LoginSession {

  private final long timeout;
  private final Object principal;
  private Set<String> roles;
  private Set<String> permissions;
  private Set<String> notRoles;
  private Set<String> notPermissions;
  private long lastAccessed;

  LoginSession(long timeout, Object principal) {
    this.timeout = timeout;
    this.principal = principal;
    touch();
  }

  synchronized void touch() {
    lastAccessed = System.currentTimeMillis();
  }

  synchronized boolean hasRole(String role) {
    touch();
    return roles().contains(role);
  }

  synchronized boolean hasNotRole(String role) {
    touch();
    return notRoles().contains(role);
  }

  synchronized boolean hasPermission(String permission) {
    touch();
    return permissions().contains(permission);
  }

  synchronized boolean hasNotPermission(String permission) {
    touch();
    return notPermissions().contains(permission);
  }

  public synchronized Object principal() {
    return principal;
  }

  synchronized void addRole(String role) {
    roles().add(role);
  }

  synchronized void addNotRole(String role) {
    notRoles().add(role);
  }

  synchronized void addPermission(String permission) {
    permissions().add(permission);
  }

  synchronized void addNotPermission(String permission) {
    notPermissions().add(permission);
  }

  synchronized long lastAccessed() {
    return lastAccessed;
  }

  synchronized long timeout() {
    return timeout;
  }

  private Set<String> roles() {
    if (roles == null) {
      roles = new HashSet<>();
    }
    return roles;
  }

  private Set<String> permissions() {
    if (permissions == null) {
      permissions = new HashSet<>();
    }
    return permissions;
  }

  private Set<String> notRoles() {
    if (notRoles == null) {
      notRoles = new HashSet<>();
    }
    return notRoles;
  }

  private Set<String> notPermissions() {
    if (notPermissions == null) {
      notPermissions = new HashSet<>();
    }
    return notPermissions;
  }


}

