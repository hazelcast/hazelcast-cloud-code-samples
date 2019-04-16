[Screenshot1]: src/site/markdown/images/Screenshot1.png "Image screenshot1.png"
[Screenshot2]: src/site/markdown/images/Screenshot2.png "Image screenshot2.png"
[Screenshot3]: src/site/markdown/images/Screenshot3.png "Image screenshot3.png"
[Screenshot4]: src/site/markdown/images/Screenshot4.png "Image screenshot4.png"

<h1>Calculation in the Hazelcast Cloud</h1>

This is an example showing one way you might connect to the Hazelcast cloud, and how you can harness it's power for improved performance.

As a problem we're going to calculate the spread of customer satisfaction, as the average doesn't give enough insight.

The example shows a custom domain model, and server-side code execution. It shows both the junior developer and senior developer approach to cloud computing, with the latter being substantially more efficient.
<h2>The problem</h2>
For this example we're going to do a statistical calculation known as <a href="https://en.wikipedia.org/wiki/Standard_deviation">Standard Deviation</a>.

Don't worry, the maths here is very easy and there are no Greek letters or fancy symbols. If you already know how this calculation works you can skip the following sub-section.
<h3>Standard Deviation explained</h3>
One thing we are often interested in is averages.

<p>For the numbers 1, 2, 3, 4 and 5 the average is 3.</p>
<p>For the numbers 2, 2, 3, 4 and 4 the average is also 3.</p>
<p>For the numbers 1, 1, 3, 5 and 5 the average is again 3.</p>

Hopefully all clear so far!

There are five numbers in each group, the total for each group is fifteen, but the actual numbers differ.

One of the things we'd like to do is produce a simple metric for how much of a spread in the numbers there is.

In the second group (2, 2, 3, 4 and 4) most of the numbers are one away from the average of 3. In the third group (1, 1, 3, 5 and 5) most of the numbers are two away from the average.

There are lots of ways to do this. Lots. The "<i>standard deviation</i>" here is just a name that means it's a way that has been adopted as a standard for calculating how numbers deviate from the average.

How it works is for each number we calculate how much this differs from the average. This difference is squared, and added to a running total.

Once we have this total, we take the average of this. So now we have the typical square of the difference from the original average.

Finally, we take the square root of this, to result in a single value that we call the standard deviation.

Let's do this calculation for the numbers 1, 2, 3, 4 and 5, which we already know has the average of 3.
<ul>
 	<li>The difference between the first number (1) and the average (3) is 2. Squaring this gives 4 which starts the running total at 4.</li>
 	<li>The difference between the second number (2) and the average (3) is 1. Squaring this gives 1, adding to the running total takes this running total to 5.</li>
 	<li>The difference between the third number (3) and the average (3) is 0. Squaring this gives 0, so the running total stays as 5.</li>
 	<li>The difference between the fourth number (4) and the average (3) is 1. Squaring this gives 1, taking the running total to 6.</li>
 	<li>The difference between the last number (5) and the average (3) is 2. Squaring this is 4, taking the running total to 10.</li>
 	<li>The running total is 10 and there were 5 numbers, so the typical square is 10 divided by 5, that is 2.</li>
 	<li>The square root of 2 gives us the <i>standard deviation</i> of 1.41.</li>
</ul>
This sort of calculation is a bit tedious, which is why we want the machines to do it. But as we're really enthusiastic, let's do this again for the numbers 1, 1, 3, 5 and 5, and their average of 3.
<ul>
 	<li>The first number (1) differs by 2 from the average. Squaring 2 starts the running total with 4.</li>
 	<li>The second number (1) also differs by 2 from the average. Squaring 2 gives 4, taking the running total to 8.</li>
 	<li>The third number (3) is the same as the average, so the square is 0 and the running total stays as 8.</li>
 	<li>The fourth number (5) differs by 2 from the average. Squaring 2 gives 4, taking the running total to 12.</li>
 	<li>The fifth number (5) again differs by 2 from the average. This adds another 4 to the running total, taking it to 16.</li>
 	<li>The running total is 16 for the 5 numbers, so the typical square is 16 / 5 = 3.2</li>
 	<li>The square root of 3.2 we all know is 1.78 which is our result.</li>
</ul>
So where we end up is the standard deviation of the numbers 1, 2, 3, 4 and 5 is 1.41. The standard deviation of the numbers 1, 1, 3, 5 and 5 is 1.78.

The standard deviation of 1.41 for the one group being lower than the standard deviation of 1.78 for the other group gives us a simple metric that proves the first group of numbers don't vary as much as the second.
<h3>The business logic</h3>
There are not many steps to the business logic.

<p>The first is to calculate the average of the input numbers.</p>
<p>The second is to calculation the running total of the square of how each number differs from the average.</p>
<p>The third is calculate the average of the running total.</p>
<p>And finally, we take the square root of that and call it the <i>standard deviation</i>.</p>
<h3>The optional magic</h3>
The optional magic is parallelism.

In the second step, we perform a calculation on each number. We take the difference for that number from the average, and then square it.

This is open to parallelism. If we have more than one CPU we could run the "square of the difference" step for several numbers at once, so long as we have a way to combine these independent answers into the running total.

This is what we'll use Hazelcast Cloud for.

We could do a similar thing in the first step too, but as it turns out we've another trick to make that even easier.
<h3>Test data</h3>
We shall stick with the five numbers 1, 2, 3, 4 and 5, since we have above the manual calculation of standard deviation resulting in 1.41.

To make it more realistic, we'll make these numbers the "satisfaction" rating from some customers in this domain object:
<pre>class Customer implements Serializable {
    private String firstName;
    private int satisfaction;
</pre>
So here what we are looking at is customer satisfaction. The average satisfaction is useful, but to know the amount of spread between very satisfied and very dissatisfied customers is even more useful.
<h2>Step 1 - The "Junior Developer" way</h2>
In the class "<code>JuniorDeveloper.java</code>", the average is calculated like this:
<pre><code>
int count = 0;
double total = 0;

for (Integer key : iMap.keySet()) {
    count++;
    total += iMap.get(key).getSatisfaction();
}

return (total / count);
</code></pre>
This is pretty easy code to understand, so that's a win from a maintenance aspect. However, it has three flaws.

The main one really is performance. This code requires every customer record to be moved from where the data is stored (in this case Hazelcast) to where the calculation is run. Even if a projection were added, this is very heavy on the network.

There is a second issue. The "<code>keySet()</code>" operation produces a collection containing all the keys to iterate across. If this collection is huge it could overflow the memory.

Lastly, there's a division by zero if map is empty. A good unit test would find this.

<b>Note</b>: Let's not forget maintenance. This code is <u>easy</u> to understand.
<h2>Step 1 - The "Senior Developer" way</h2>
In the class "<code>SeniorDeveloper.java</code>", the average is calculated in one line:
<pre><code>
return iMap.aggregate(Aggregators.integerAvg("satisfaction"));
</code></pre>
Hazelcast provides in-built functions for typical calculations, so why not use them ?

Senior developers do more thinking and less typing.

Hazelcast's implementation uses is the same as the coding for senior developers in step 2, but there are some internal optimisations so you won't be able to beat it with your own coding.

<b>Note</b>: Let's never forget maintenance. Even if you don't know Hazelcast, it's fairly intuitive what is happening.

<h2>Step 2 - The "Junior Developer" way</h2>
In "<code>JuniorDeveloper.java</code>", the sum of the square of the differences is calculated as:
<pre><code>
double total = 0;

for (Integer key : iMap.keySet()) {
    int satisfaction = iMap.get(key).getSatisfaction();
    double difference = satisfaction - average;
    total += difference * difference;
}

return total;
</code></pre>
This is not much different from the junior developer's approach to step 1. The coding is pretty clear, lots of data is retrieved and the "<code>keySet()</code>" operation may still overflow memory.

At least we don't have to worry about division by zero. Unit tests will pass.

<b>Note</b>: Maintenance! So from a maintenance perspective this is better. Less faults than the junior developer's approach to step 1 and very clear.

<h2>Step 2 - The "Senior Developer" way</h2>
This is where the magic starts to happen.

In Hazelcast, we can submit a Java "<code>Runnable</code>" or "<code>Callable</code>" task to run on the grid. We can select to run this task on one, some or all of the grid servers. So that's what we do here.

The client uses Hazelcast's <a href="https://docs.hazelcast.org/docs/3.12/javadoc/com/hazelcast/core/IExecutorService.html">ExecutorService</a> to run a "<code>Callable</code>" on each grid server.

The callable itself is imaginatively named "<code>TotalDifferenceSquaredCallable.java</code>" and it runs this calculation on the Hazelcast server where it is invoked.
<pre><code>
double total = 0;

for (Integer key : iMap.localKeySet()) {
    int satisfaction = iMap.get(key).getSatisfaction();
    double difference = satisfaction - this.average;
    total += difference * difference;
}

return total;
</code></pre>

This is more or less the same calculation as in "<code>JuniorDeveloper.java</code>"'s "<code>totalDifferenceSquared()</code>" method. We could add a stream or a lambda but it wouldn't be really any different.

Where it <b>is</b> different is in three ways.
<ul>
 	<li>Firstly, this calculation is parallelised. The Hazelcast client on your machine does this:
<pre><code>
Map&lt;Member, Future&gt; results =
    executorService.submitToAllMembers(totalDifferenceSquaredCallable);
</code></pre>
The task is sent to all grid servers in parallel.

Each server calculates a sub-total only for the data that server hosts. If you have two servers, the calculation takes half the time. If you have ten servers, the calculation takes a tenth of the time. Each server runs it's calculation independently, the run time is dependent on how much data each server hosts rather than how much data as a whole exists.</li>
 	<li>Secondly, to do this the task uses the "<code>localKeySet()</code>" method instead of the "<code>keySet()</code>" method.This method returns the keys that are held by the current process. And since we run the task on all processes we cover all the keys.

This makes a big difference.

We are only looking at the keys, and therefore entries, hosted by the current process so we don't have to do any network call to retrieve them from elsewhere.

And of course, the "<code>localKeySet()</code>" operation is asking for only the keys from an "<code>EntrySet</code>" hosted in local memory. The local "<code>KeySet</code>" must be smaller than the local "<code>EntrySet</code>" since we are ignoring the values so we should be less concerned about overflowing memory. The local "<code>KeySet</code>" returns a defensive copy of the "<code>EntrySet</code>" so it is not impossible. </code></li>
 	<li>Finally, the task is submitted across every member in the cluster, which gives us a collection of "<code>Future</code>" objects to manage ("<code>Map&lt;Member, Future&gt; results</code>"). There is one "<code>Future</code>" for each member running the task, and we need to wait for them all to finish.This is not exactly difficult, and since all members will have roughly the same amount of data we can assume execution time is equal and iterate across this collection running "<code>Future.get()</code>" to obtain the sub-total from each member.</li>
</ul>
<b>Note</b>: Maintenance, one more time. And this is a tough one. This coding has some obvious bits but some other parts need a bit of Hazelcast knowledge. Is the performance boost worth the cost of understanding ?
<h2>Run time complexity</h2>
So what actually is the difference between the two ways ?

There are 5 data records, and the simplest Hazelcast cluster has 1 server.

The simple way requires 5 integers to be retrieved for the "<code>satisfaction</code>" field to run into the calculation. For simplicity we obtain the whole data record for each.

The clever way runs the calculation on the 1 server and returns 1 double across the network.

So the choice is 5 network transfers or 1. 1 is obviously less than 5 but does this really matter ?

Now imagine there are 1,000,000 data records and we chose to have 2 Hazelcast servers. One Hazelcast server could handle this volume but wouldn't be resilient to failure.

So the simple way now retrieves 1,000,000 integers across the network and the clever way retrieves 2 doubles (one from each server).

It's pretty clear that <b>if</b> data volumes scale, the server-side computation copes better. The same amount of data still has to be examined, but less data moves.
<h2>Do not forget concurrency</h2>
The coding in this example disregards concurrency. Generally a bad idea unless you know what you are doing.

Step 1 calculates the average, then step 2 calculates the deviation from the average.

It is possible that records are added or removed between step 1 and 2, which makes the calculation wrong.

While there are ways to deal with changing data, here it is almost axiomatic that the data cannot change when an average calculation is being used.
<h2>Running the example</h2>
To keep things clear, we'll create a new cluster without any special customisation. It doesn't matter what we call it, so we will go with "<b>stones</b>" as a name.

![Image of creating a cluster in Hazelcast Cloud][Screenshot1]

This'll take a few seconds to create, and once done click on the "<b>Configure Client</b>" button to find out the connection credentials. This will pop up a screen like the below.

![Image of cluster credentials in Hazelcast Cloud][Screenshot2]

What we need to note is the three fields at the bottom. We need the cluster name ( "<i>stones</i>"), password and discovery token fields. The password and discovery token fields are hidden until you click on the "eye" symbol. Your values will differ of course.

Now we can build and run the client.
<pre><code>
mvn install
java -jar target/stdev-0.1-SNAPSHOT.jar 
</code></pre>
When you start it, you'll get asked for these three fields:

![Image of Java process asking for credentials][Screenshot3]

Once these are provided, processing will begin:

![Image of Java process output][Screenshot4]
<h2>Code injection</h2>
There is one last point of technical interest. If we are running server-side processing, how do the servers get the code to process ?

Typically what we do to "deploy" code is we copy it onto the filesystem on the server machines, and bounce the servers to pick up the code from that filesystem.

This is ok, but it's not exactly great for high availability.

In Java terms, what this means is we are asking the JVM's class-loader to read in classes from the filesystem. So we are reading in Java classes from the filesystem, streaming bytes from disk into memory.

Why not stream these bytes from somewhere else ?

Streaming bytes is all in a day's work for Hazelcast !

So what we do here is get our Hazelcast client to send the "<code>Customer.class</code>" and the "<code>TotalDifferenceSquaredCallable.class</code>" to the Hazelcast cluster. This makes these classes available to the class-loader, and therefore to the Hazelcast server. This happens without a restart, so this is particularly important if you only have one Hazelcast server process.

If you are interested, the code to do this is in the "<code>ApplicationConfig.java</code>" file, and looks like this:
<pre><code>
ClientUserCodeDeploymentConfig clientUserCodeDeploymentConfig = clientConfig.getUserCodeDeploymentConfig();

clientUserCodeDeploymentConfig.setEnabled(true);

clientUserCodeDeploymentConfig.addClass(TotalDifferenceSquaredCallable.class);
clientUserCodeDeploymentConfig.addClass(Customer.class);
</code></pre>
We adjust the client's configuration to say it can send Java classes to the servers, and which particular ones to send.
<h2>Summary</h2>
This example shows Hazelcast Cloud can be more than a data store or cache. If you want it to be, it can be a compute grid.

This is demonstrated by an example where the business logic is shown the "junior developer" way and the "senior developer" way.

On a cluster with 2 Hazelcast servers, the senior developer's way is twice as fast. Expand to 3 servers and now it's 3 times faster than the junior developer's way. Scalable speed.

One way is better than the other, but which is better depends on whether you value performance or simplicity. The skill of the senior developer is in knowing the choices and using the right one.

Find the sample code <a href="https://github.com/hazelcast/hazelcast-cloud-code-samples/tree/master/compute/stdev">here</a> with test data with an average customer satisfaction of 3 with a standard deviation of 1.41. For customer satisfaction the average is an important metric but so also is how much variation there is from average, as very unhappy customers consume disproportionate amounts of time.

