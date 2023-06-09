/* This file is part of VoltDB.
 * Copyright (C) 2008-2022 Volt Active Data Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * <h1>What is the package org.voltdb.planner?</h1>
 * <p>This package contains the cost-based, flexible, distributed
 *   planner/optimizer for VoltDB.</p>
 * <p>
 * The execution of any query is a some kind of a scan.  By <em>scan</em> we mean a loop,
 * or perhaps a nested loop.  The most naive, and slowest, scan would be just a scan of a table.
 * Each row of the table is fetched, evaluated and written to the output or else skipped, depending
 * the filters of join conditions or the WHERE clause.  A more sophisticated method would be
 * to scan an index.  This scans only a fraction of the rows.  But it's still a scan.</p>
 *
 * <p>
 * The planner creates <em>plans</em>.  A plan is a kind of description of the execution of
 * a query.  It may help to think of a plan as an executable program file, say an ELF or
 * a.out file.  The plan contains the instructions needed to do the computation, but the
 * plan doesn't have any resources to do any calculations.  The plan is just a data structure.</p>
 *
 * <p>
 * Each plan is made of a tree of <em>plan nodes</em>.  Each plan node describes
 * some computation which operates on tables and produces a single table.  The result table
 * and the argument tables may be persistent tables or they may be temporary tables.  Temporary
 * tables created during the execution of a plan are destroyed after the execution of the
 * plan.</p>
 *
 * <p>
 * For example, a SEQUENTIAL_SCAN node describes a computation which operates on a
 * table, reads each row of the table and sends it to the output, possibly filtering
 * with a predicate or applying a limit or an offset.  A NESTED_LOOP plan node describes
 * a join computation, either an inner or outer join.
 * This join computation will be given two tables which must be scanned
 * in two nested loops.  For each row of the left hand table, which we call the <em>outer</em>
 * table, we look at each row of the right hand table, which we call the <em>inner</em>
 * table.  If the pair of rows satisfy the join condition, we write the row to the
 * output table.</br>
 * Note:  The computation for NESTED_LOOP is somewhat more complicated
 * than this, as we may be able to skip the scan of the inner table.  There
 * may be other optimizations.</br>
 * Note Also: The tree formed by a plan is not very similar to the parse tree formed
 * from a SQL or DDL statement.  The plan relates to the SQL text approximately the
 * same way an assembly language program relates to a string of C++ program text.</p>
 *
 * <p>
 * We say a plan node <em>describes</em> a computation because the plan node does
 * not actually have the code to do the computation.  Plan nodes are generated by
 * the planner in the Java code in the VoltDB server.  Plans are transmitted to the
 * Execution Engine, or EE, in the form of JSON objects, represented as strings.
 * The EE has its own definitions for all the plan nodes in C++.  The
 * EE can reconstruct the the plan in a C++ data structure from the JSON.  The EE also
 * has a hierarchy of classes called <em>executor classes</em> which is parallel to
 * the plan node hierarchy.  Executor class objects contain the code which does
 * the real computations of the plan nodes.  Most dynamic data, such as table
 * data, are stored in the executor objects.</p>
 *
 * <p>
 * The children of a plan
 * node describe computations which produce temporary tables.  The parents of a plan
 * node describe computations which consume the result of the plan node.  So, child
 * nodes could also be called <em>producer</em> nodes, and parent nodes could also
 * be called <em>consumer</em> nodes.</br>
 * Note: A node may have only one parent node.  A node may, therefore, create only
 * one value.</p>
 *
 * <p>The output schema of a plan node gives the set of columns which the computation
 * of the plan node will create.  For each row which is not rejected, some computation
 * is done to the row, and the results are put into an output row.
 *
 * <p>
 * Some operations can be computed on a row-wise basis.  For example, if
 * a table has N columns, but only M < N columns are used in the output, the projection
 * from N columns to M columns happens independently for all rows.
 * Similarly, an output column can be a function of input columns.  These
 * functions can by computed row-wise.  Finally, most
 * aggregate functions can be computed row-wise.  For example, the sum
 * aggregate function needs only a running sum and values from the current row
 * to update its state.  Not all operations can be computed row-wise.
 * For example, a sort requires an entire table.
 * For row-wise operations, we avoid creating extra temporary
 * tables by combining scans. The row-by-row operations are placed in <em>inline</em>
 * nodes in a scan node.  The inline nodes are applied to each row of
 * the scan which passes the filters during the scan.  The inline nodes have
 * their own output columns, as discussed above.</p>
 *
 * <p>
 * The classes of plan nodes are organized in a hierarchy.  The root of the
 * hierarchy is the AbstractPlanNode class.
 * </p>
 *
 * <h1>How Does The Planner Work In Detail?</h1>
 * <p>
 * The planner takes a data structure describing a DML or DQL SQL
 * statement.  It produces a plan of execution which can be used to
 * evaluate the statement.  The Execution Engine executes the plan.
 * Note that DDL is processed entirely differently, and is generally
 * not part of this discussion.  DDL which has embedded DQL or DML
 * will need to be planned, as detailed here.  For example, materialized
 * views, single statement procedure definitions and delete statement
 * limit plans are DDL statements which have plannable elements.</p>
 * <h2>The Input</h2>
 * <p>
 * The input to the planner is originally a SQL statement, which is a
 * text string.  The HSQL front end translates the user’s text into an
 * HSQL data structure which represents the statement.  This data
 * structure is translated into a second data structure called
 * VoltXML, which is similar to XML.  VoltXML has elements, called
 * VoltXMLElements, and elements have string valued attributes, just
 * like XML.  There is no support for namespaces in VoltXML, though it
 * is hardly needed.  This VoltXML is translated again into another
 * data structure called AbstractParsedStatement and its subclasses.
 * This AbstractParsedStatement, along with a catalog defining the
 * tables, indexes and other DDL objects, is the input to the
 * planner.</p>
 *
 * <p>
 * An AbstractParsedStatement object contains several pieces.
 * <ol>
 *   <li>All the parts of the user’s original SQL statement,
 *       including
 *     <ol>
 *       <li>The display list,</li>
 *       <li>The list of joined tables, including
 *         <ol>
 *           <li>Joined tables,</li>
 *           <li>Derived tables, and</li>
 *           <li>expressions used in WHERE and ON conditions,</li>
 *         </ol>
 *       </li>
 *       <li>The list of GROUP BY expressions,</li>
 *       <li>The HAVING expression, if any,</li>
 *       <li>Any LIMIT or OFFSET constants.</li>
 *       <li>An indication of whether the statement is a SELECT, DELETE
 *           INSERT, UPSERT or UPDATE statement, or a set operation like
 *           UNION, or INTERSECT.  As we said
 *           before, DDL is generally not planned, but some parts of
 *           DDL statements may require planning.</li>
 *     </ol>
 *   </li>
 * </ol>
 * Note that a joined or derived table is associated with a tablescan,
 * and that the planner generally works with tablescans rather than
 * tables.  For example, in this SQL statement:
 *   <pre>
 *    select *
 *    from t as left join t as right
 *    on left.a = right.a;
 *   </pre>
 * the aliases <code>left</code> and <code>right</code> name
 * the table T, which is scanned twice independently as the
 * left and right scans of the join.</p>
 *
 * <p>
 * The SQL standard and HSQL refer to tablescans as range
 * variables, but the planner calls them tablescans.  These are
 * essentially a table with an alias to distinguish between different
 * references of the same table in a self join.</p>
 *
 * <h2>The Plan Nodes In Detail</h2>
 * <p>
 * Plan nodes compute some operation on one or more input tables.  For
 * example, an ORDERBY plan node sorts its only input table.  A
 * NESTEDLOOPJOIN node takes two tables as input and computes a join
 * in a nested loop, scanning the outer table in the outer loop, and
 * the inner table in the inner loop.</p>
 *
 * <p>
 * There are about 24 different plan nodes, organized in five categories.  The categories are:
 * <ul>
 *   <li>Operator nodes, for executing DML operations such as DELETE, INSERT and UPDATE,</li>
 *   <li>Scan nodes for scanning tables and indexes,</li>
 *   <li>Join nodes for calculating different kinds of joins,</li>
 *   <li>Communication nodes for sending result tables to the
 *       coordinator or receiving results from distributed executions,</li>
 *   <li>Miscellaneous nodes.  These include
 *      <ul>
 *        <li>Nodes to compute aggregates and GROUP BY.  The aggregation algorithms include
 *          <ul>
 *            <li>Serial Aggregation, which is aggregation based on sorting,</li>
 *            <li>Hash Aggregation, which is aggregation based on hash tables,</li>
 *            <li>Partial Aggregation, which is a form of parallelized
 *                serial aggregation.</li>
 *          </ul>
 *       </li>
 *       <li>Nodes to compute set operations, like UNION or INTERSECT,</li>
 *       <li>Nodes to compute ORDERBY,</li>
 *       <li>Nodes to compute PROJECTION,</li>
 *       <li>Nodes to compute MATERIALIZE,</li>
 *       <li>Nodes to compute LIMIT and OFFSET,</li>
 *       <li>Nodes to compute WINDOWFUNCTION.</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 *
 * <h2>The Execution Model</h2>
 * <h3>Plans and Fragments</h3>
 * <p>
 * The plan nodes are connected in one or two fragments.  Each
 * fragment is a plan node tree.
 * <ul>
 *   <li>A one-fragment plan is a single partition plan.  It is
 *       scheduled on a particular partition, and the resulting table is
 *       sent to the user.</li>
 *   <li>A two-fragment plan is a multi partition plan.  The two
 *       fragments are called the coordinator fragment and the distributed
 *       fragment.</li>
 *   <li>The distributed fragment is scheduled to be sent to all the
 *       partitions and executed in parallel on the partitions.</li>
 *   <li>The results of the executions of the distributed fragment on
 *       each of the partitions are sent to the coordinator node which
 *       executes the coordinator fragment.  The coordinator fragment
 *       has a RECEIVE or a MERGERECEIVE node which knows how to
 *       combine the distributed results into a single table.</li>
 *   <li>Plan node execution is similar for both the distributed and
 *       coordinator fragment.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Plan nodes have different kinds of children.  A plan node can have
 * child nodes which are out-of-line or inline.  The latter are used to
 * compute which can be computed row-wise, in a single pass through
 * the input table along with the parent node.  For example, a LIMIT
 * or OFFSET node is often made inline, since it just counts how many
 * initial rows to allow or ignore.  PROJECTION nodes can also be
 * inlined, since they operate only row-wise.  An ORDERBY node can
 * generally not be inlined, since the entire table must be present to
 * sort it.  But if the ORDERBY node’s input table comes from a
 * RECEIVE node, and the planner can tell that the input is sorted,
 * then RECEIVE node can be replaced with a MERGERECEIVE node and the
 * ORDERBY node can be inlined in the MERGERECEIVE node.  The
 * MERGERECEIVE node gets sorted input tables from distributed
 * fragments.  The MERGERECEIVE node merges the input tables by
 * choosing the first value of all its input tables which is smallest.
 * Indeed, the MERGERECEIVE node must have an inline ORDERBY node to
 * carry the ordering expressions. </p>
 *
 * <h3>Fragment initialization</h3>
 * <p>
 * The plan nodes have a method for initialization.  They first
 * initialize their children, then they initialize themselves.</p>
 *
 * <h3>Fragment Execution</h3>
 * <p>
 * A fragment tree executes the operations of each of its plans,
 * starting with the leaves.  At each node the execution function reads rows from
 * the input tables, computes a function into a single row, perhaps
 * alters that row with inline plan nodes and writes the output row to
 * a single output table.  A plan node can only have a single parent and a
 * single output table.</p>
 *
 * <h2>The Planner’s Passes</h2>
 * <p>
 * The planner doesn’t have passes in the way a compiler might.  It’s
 * more of a pass itself.  But it does have several phases.
 * <ol>
 *   <li>First, the planner calculates some join orders.  The
 *       statement has a set of tablescans, and the efficiency of a
 *       plan depends on the order in which the tablescans are
 *       scanned.  There is a large amount of confusing code devoted
 *       to this, but essentially each possible order of the
 *       tablescans is generated.  There is a limit to the size of
 *       this order set.  If there are more than 5 tables, the order
 *       in the statement is used, to avoid exponential explosion.
 *       A java stored procedures, may also have an explicit
 *       user-provided join order specification.</li>
 *   <li>Each join order results in a join tree, whose java type is
 *       JoinNode.
 *     <ol>
 *       <li>The subtypes of JoinNode are SubqueryLeafNode, BranchNode
 *         and TableLeafNode.  These represent derived tables, which
 *         we often call subqueries, joined tables and tablescans
 *         respectively.</li>
 *       <li>We do some analysis of the join predicates, to
 *         distribute them to the proper JoinNode nodes.  For example,
 *           <ul>
 *             <li>
 *               If an expression contains only references to a single
 *               tablescan, that expression can be placed along with
 *               the TableLeafNode for the tablescan, to know if the
 *               expression can be used with an index.</li>
 *             <li>
 *               Similarly, if the tablescan is the outer scan of a
 *               join, it can also be added to an outer-only list in
 *               the join node.  In this case we will know to evaluate
 *               these outer-only nodes when scanning and not scan the
 *               inner table at all if the expression’s evaluation is
 *               false.</li>
 *             <li>
 *               Expressions which reference both branches of a join
 *               might be outer expressions of a higher level
 *               join.</li>
 *             <li>For OUTER joins, the differing semantics of ON clauses
 *                 and WHERE clauses also factor into where in the join
 *                 tree a predicate should apply.</li>
 *           </ul>
 *       </li>
 *       <li>
 *         The planner calculates a set of access paths for the tablescans.
 *         Note that this does not depend on the join order.</li>
 *         <ol>
 *           <li>
 *             These access paths will tell how the tablescan will be
 *             scanned.  It may be scanned directly or through an index.</li>
 *           <li>
 *             The scan associated with the access path may provide
 *             ordering for a statement level order by or a window
 *             function.  This is stored in the access path as well.</li>
 *           <li>
 *             For each join order, the planner creates a subplan in
 *             SubPlanAssembler.  This class traverses the join tree
 *             and, consulting with the access paths, turns the
 *             TableLeafNode and SubqueryLeafNode join nodes into some
 *             kind of AbstractScanPlanNode, and turns the BranchNodes
 *             into some kind of AbstractJoinPlanNode.  The result is a
 *             set of subplans.  These are just trees of scans and
 *             joins.  The other parts of the statement, order by,
 *             group by, limit, offset, having, and DML operations
 *             table insertion, update and deletion are not represented
 *             here.  In particular, if the plan is a multi-partition
 *             plan, it needs a SEND/RECEIVE pair to designate the
 *             boundary between the coordinator and distributed plans.
 *             But this pair will not be part of the subplan.</li>
 *           <li>
 *             For each subplan, the plan assembler in PlanAssembler
 *             adds new nodes on the top of the subplan tree, to form
 *             the complete tree.  These may include:
 *             <ul>
 *               <li>A SEND node on the top of the plan, to send the
 *                   result to the server.</li>
 *               <li>SEND/RECEIVE pairs in multi-partition queries,</li>
 *               <li>Aggregate nodes, to calculate GROUP BY groups and
 *                   aggregate functions.</li>
 *               <li>Window function nodes to calculate window
 *                   functions,</li>
 *               <li>ORDERBY nodes to implement sorting.</li>
 *               <li>PROJECTION nodes to calculate display lists.</li>
 *             </ul>
 *             All but the first two are optional.</li>
 *         </ol>
 *       </li>
 *     </ol>
 *   </li>
 * </ol>
 * </p>
 *
 * <p>
 * Note that the transformation from ORDERBY->RECEIVE to
 * MERGERECEIVE(ORDERBY inlined) discussed above is not done in the
 * plan assembler.  Also, sometimes nodes which could be made inline
 * are not made so by the plan assembler.  These and other final
 * transformations could be performed in the plan assembler.  But it
 * seems simpler to do them locally, after the plan tree is formed.</p>
 *
 * <h2>The Microoptimizer</h2>
 * <p>
 * The Microoptimizer takes a plan and applies various transformations.  The current set of transformations include:
 * <ol>
 *   <li>
 *     Pushing limit and offset nodes into distributed fragments and
 *     making them inline.  If a statement has LIMIT 100, for example,
 *     there is no reason for distributed fragments to generate more
 *     than 100 rows.  Not all of these may be used.</li>
 *   <li>
 *     Transforming plans with ORDERBY, LIMIT and OFFSET into plans
 *     with MERGERECEIVE and inline ORDERBY, LIMIT and OFFSET plan
 *     nodes.</li>
 *  <li> Make aggregate nodes be inline.
 *    <ol>
 *      <li>
 *        A hash aggregate node reads each input row, calculates group
 *        by keys and a hash value from this calculation.  The hash
 *        aggregate node puts the group by key value into a hash
 *        table.  It also updates the aggregate function values.  For
 *        example, if GBV1 and GBV2 are a group by keys, then the
 *        expression MIN(GBV1 * GBV2) would compute the minimum of
 *        the product of GBV1 and GBV2.   This is a row-wise
 *        operation.</li>
 *      <li>
 *        A serial aggregate node reads each input row and calculates
 *        the group by keys.   The input table will be sorted, so if
 *        the group by key values change we know we have seen an
 *        entire group and output a row.  In any case we update
 *        aggregate values, like the MIN(GBV1*GBV2) expression above.
 *        So serial aggregate nodes can be calculated row-wise.</li>
 *      <li>
 *        Partial aggregation is a kind of serial aggregation.</li>
 *    </ol>
 *   So,  the aggregate node’s calculation can be done row-wise.  This
 *   means the aggregate node can be inlined in some cases.</li>
 *   <li>
 *    Replace an aggregate plan node with an index count plan node or
 *    a table count plan node.</li>
 *   <li>
 *     Replace an aggregate plan node with an index scan plan node with
 *    an inline limit node.</li>
 * </ol>
 * </p>
 */
package org.voltdb.planner;
