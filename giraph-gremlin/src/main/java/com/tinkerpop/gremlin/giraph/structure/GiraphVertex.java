package com.tinkerpop.gremlin.giraph.structure;

import com.tinkerpop.gremlin.giraph.process.computer.util.GiraphComputerHelper;
import com.tinkerpop.gremlin.giraph.process.graph.step.sideEffect.GiraphGraphStep;
import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.graph.step.filter.HasStep;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.StartStep;
import com.tinkerpop.gremlin.process.graph.step.util.IdentityStep;
import com.tinkerpop.gremlin.process.graph.util.DefaultGraphTraversal;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Compare;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.MetaProperty;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.HasContainer;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;
import com.tinkerpop.gremlin.tinkergraph.structure.TinkerMetaProperty;
import com.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GiraphVertex extends GiraphElement implements Vertex, Serializable, WrappedVertex<TinkerVertex> {

    protected GiraphVertex() {
    }

    public GiraphVertex(final TinkerVertex vertex, final GiraphGraph graph) {
        super(vertex, graph);
    }

    @Override
    public <V> MetaProperty<V> property(final String key) {
        final MetaProperty<V> metaProperty = getBaseVertex().<V>property(key);
        return metaProperty.isPresent() ?
                new GiraphMetaProperty<>((TinkerMetaProperty<V>) ((Vertex) this.tinkerElement).property(key), this) :
                MetaProperty.<V>empty();
    }

    @Override
    public <V> MetaProperty<V> property(final String key, final V value) {
        throw Element.Exceptions.propertyAdditionNotSupported();
    }

    @Override
    public Edge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
        throw Vertex.Exceptions.edgeAdditionsNotSupported();
    }

    @Override
    public GraphTraversal<Vertex, Vertex> start() {
        final GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<Vertex, Vertex>(this.graph) {
            @Override
            public GraphTraversal<Vertex, Vertex> submit(final GraphComputer computer) {

                GiraphComputerHelper.prepareTraversalForComputer(this);
                final String label = this.getSteps().get(0).getLabel();
                TraversalHelper.removeStep(0, this);
                final Step identityStep = new IdentityStep(this);
                if (TraversalHelper.isLabeled(label))
                    identityStep.setLabel(label);

                TraversalHelper.insertStep(identityStep, 0, this);
                TraversalHelper.insertStep(new HasStep(this, new HasContainer(Element.ID, Compare.EQUAL, tinkerElement.id())), 0, this);
                TraversalHelper.insertStep(new GiraphGraphStep<>(this, Vertex.class, graph), 0, this);

                return super.submit(computer);
            }
        };
        return (GraphTraversal) traversal.addStep(new StartStep<>(traversal, this));
    }

    @Override
    public TinkerVertex getBaseVertex() {
        return (TinkerVertex) this.tinkerElement;
    }

    @Override
    public Vertex.Iterators iterators() {
        return this.iterators;
    }

    private final Vertex.Iterators iterators = new Iterators();

    protected class Iterators implements Vertex.Iterators, Serializable {

        @Override
        public Iterator<Vertex> vertices(final Direction direction, final int branchFactor, final String... labels) {
            return StreamFactory.stream(getBaseVertex().iterators().vertices(direction, branchFactor, labels)).map(v -> graph.v(v.id())).iterator();
        }

        @Override
        public Iterator<Edge> edges(final Direction direction, final int branchFactor, final String... labels) {
            return StreamFactory.stream(getBaseVertex().iterators().edges(direction, branchFactor, labels)).map(e -> graph.e(e.id())).iterator();
        }

        @Override
        public <V> Iterator<MetaProperty<V>> properties(final String... propertyKeys) {
            return (Iterator) StreamFactory.stream(getBaseVertex().iterators().properties(propertyKeys))
                    .map(property -> new GiraphMetaProperty<>((TinkerMetaProperty<V>) property, GiraphVertex.this)).iterator();
        }

        @Override
        public <V> Iterator<MetaProperty<V>> hiddens(final String... propertyKeys) {
            return (Iterator) StreamFactory.stream(getBaseVertex().iterators().hiddens(propertyKeys))
                    .map(property -> new GiraphMetaProperty<>((TinkerMetaProperty<V>) property, GiraphVertex.this)).iterator();
        }
    }
}
