/**
 * Copyright 2004 - 2016 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.syncleus.ferma;

public class RunMe {
    public static void main(String[] args) {
        System.out.println("implementation version: " + RunMe.class.getPackage().getImplementationVersion());
        System.out.println("implementation vendor: " + RunMe.class.getPackage().getImplementationVendor());
        System.out.println("implementation title: " + RunMe.class.getPackage().getImplementationTitle());
        System.out.println("Specification version: " + RunMe.class.getPackage().getSpecificationVersion());
        System.out.println("Specification vendor: " + RunMe.class.getPackage().getSpecificationVendor());
    }
}
