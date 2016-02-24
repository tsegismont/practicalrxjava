/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package practicalrxjava;

import rx.Observable;

/**
 * @author Thomas Segismont
 */
public class DataTransform {

  public static void main(String[] args) {

    Observable.range(1, 100)
      .reduce(0, (a, b) -> a + b)
      .subscribe(i -> System.out.println("i = " + i));

  }

}
