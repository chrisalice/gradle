/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.dsl

import groovy.transform.NotYetImplemented
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.EnableModelDsl

import static org.gradle.util.Matchers.containsText

class ModelDslCreationIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        EnableModelDsl.enable(executer)
    }

    def "can create and initialize elements"() {
        when:
        buildScript """
            @Managed
            interface Thing {
                String getName()
                void setName(String name)
            }

            model {
                thing1(Thing) {
                    name = "foo"
                }
                tasks {
                    create("echo") {
                        doLast {
                            println "thing1.name: " + \$("thing1.name")
                        }
                    }
                }
            }
        """

        then:
        succeeds "echo"
        output.contains "thing1.name: foo"
    }

    def "creator closure can reference inputs"() {
        when:
        buildScript """
            @Managed
            interface Thing {
                String getName()
                void setName(String name)
            }

            model {
                thing1(Thing) {
                    name = "foo"
                }
                thing2(Thing) {
                    name = \$("thing1.name") + " bar"
                }
                tasks {
                    create("echo") {
                        doLast {
                            println "thing2.name: " + \$("thing2.name")
                        }
                    }
                }
            }
        """

        then:
        succeeds "echo"
        output.contains "thing2.name: foo bar"
    }

    @NotYetImplemented
    def "creator closure can reference inputs using relative property reference"() {
        when:
        buildScript """
            @Managed
            interface Thing {
                String getName()
                void setName(String name)
            }

            model {
                thing1(Thing) {
                    name = "foo"
                }
                thing2(Thing) {
                    name = "\${thing1.name} bar" // reference in a gstring
                }
                tasks {
                    create("echo") {
                        doLast {
                            println "thing2.name: " + thing2.name
                        }
                    }
                }
            }
        """

        then:
        succeeds "echo"
        output.contains "thing2.name: foo bar"
    }

    def "can create elements without mutating"() {
        when:
        buildScript """
            @Managed
            interface Thing {
                String getName()
                void setName(String name)
            }

            model {
                thing1(Thing)
                tasks {
                    create("echo") {
                        doLast {
                            println "thing1.name: " + \$("thing1.name")
                        }
                    }
                }
            }
        """

        then:
        succeeds "echo"
        output.contains "thing1.name: null"
    }

    def "can apply defaults before creator closure is invoked"() {
        when:
        buildScript """
            @Managed
            interface Thing {
                String getName()
                void setName(String name)
            }

            class MyPlugin extends RuleSource {
                @Defaults
                void applyDefaults(Thing thing) {
                    thing.name = "default"
                }
            }

            apply plugin: MyPlugin

            model {
                thing1(Thing) {
                    name = "\$name foo"
                }
                tasks {
                    create("echo") {
                        doLast {
                            println "thing1.name: " + \$("thing1.name")
                        }
                    }
                }
            }
        """

        then:
        succeeds "echo"
        output.contains "thing1.name: default foo"
    }

    def "cannot create non managed types"() {
        when:
        buildScript """
            interface Thing {
                String getName()
                void setName(String name)
            }

            model {
                thing1(Thing) {
                    name = "foo"
                }
                tasks {
                    create("echo") {
                        doLast {
                            println "thing1.name: " + \$("thing1.name")
                        }
                    }
                }
            }
        """

        then:
        fails "dependencies" // something that doesn't actually require thing1 to be built
        failure.assertThatCause(containsText("model.thing1 @ build.gradle"))
        failure.assertThatCause(containsText("Cannot create an element of type Thing as it is not a managed type"))
    }
}
