/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.ast;

import java.io.IOException;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleSequence;


/**
 * An instruction to visit the children of a node.
 */
final class RecurseNode extends TemplateElement {
    
    Expression targetNode, namespaces;
    
    RecurseNode(Expression targetNode, Expression namespaces) {
        this.targetNode = targetNode;
        this.namespaces = namespaces;
    }

    @Override
    TemplateElement[] accept(Environment env) throws IOException, TemplateException {
        TemplateModel node = targetNode == null ? null : targetNode.eval(env);
        if (node != null && !(node instanceof TemplateNodeModel)) {
            throw new NonNodeException(targetNode, node, "node", env);
        }
        
        TemplateModel nss = namespaces == null ? null : namespaces.eval(env);
        if (namespaces instanceof StringLiteral) {
            nss = env.importLib(((TemplateScalarModel) nss).getAsString(), null);
        } else if (namespaces instanceof ListLiteral) {
            nss = ((ListLiteral) namespaces).evaluateStringsToNamespaces(env);
        }
        if (nss != null) {
            if (nss instanceof TemplateHashModel) {
                SimpleSequence ss = new SimpleSequence(1);
                ss.add(nss);
                nss = ss;
            } else if (!(nss instanceof TemplateSequenceModel)) {
                if (namespaces != null) {
                    throw new NonSequenceException(namespaces, nss, env);
                } else {
                    // Should not occur
                    throw new _MiscTemplateException(env, "Expecting a sequence of namespaces after \"using\"");
                }
            }
        }
        
        env.recurse((TemplateNodeModel) node, (TemplateSequenceModel) nss);
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (targetNode != null) {
            sb.append(' ');
            sb.append(targetNode.getCanonicalForm());
        }
        if (namespaces != null) {
            sb.append(" using ");
            sb.append(namespaces.getCanonicalForm());
        }
        if (canonical) sb.append("/>");
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#recurse";
    }

    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return targetNode;
        case 1: return namespaces;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.NODE;
        case 1: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
}