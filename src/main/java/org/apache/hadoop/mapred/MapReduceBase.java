/**
 * Copyright 2006 The Apache Software Foundation
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

package org.apache.hadoop.mapred;

import java.io.IOException;

import org.apache.hadoop.io.Closeable;
import org.apache.hadoop.mapred.JobConfigurable;

/** Base class for {@link org.apache.hadoop.mapred.Mapper} and {@link org.apache.hadoop.mapred.Reducer} implementations.
 * Provides default implementations for a few methods.
 *
 * @author Owen O'Malley
 */
public class MapReduceBase implements Closeable, JobConfigurable {

  /** Default implementation that does nothing. */
  public void close() throws IOException {
  }

  /** Default implementation that does nothing. */
  public void configure(JobConf job) {
  }

}
