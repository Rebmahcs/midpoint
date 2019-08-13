/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.prism;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 *  Represents visitable JAXB bean.
 *
 *  EXPERIMENTAL. Consider merging with traditional prism Visitable.
 */
@FunctionalInterface
public interface JaxbVisitable {

	void accept(JaxbVisitor visitor);

	static void visitPrismStructure(JaxbVisitable visitable, Visitor prismVisitor) {
		if (visitable instanceof Containerable) {
			((Containerable) visitable).asPrismContainerValue().accept(prismVisitor);
		} else if (visitable instanceof Referencable) {
			PrismObject<?> object = ((Referencable) visitable).asReferenceValue().getObject();
			if (object != null) {
				object.accept(prismVisitor);
			}
		} else if (visitable instanceof RawType) {
			RawType raw = (RawType) visitable;
			if (raw.isParsed()) {
				raw.getAlreadyParsedValue().accept(prismVisitor);
			}
		}
	}
}