# ABTest

## Project Overview
This data project will focus on the development of components for a scalable, 
fault-tolerant A/B testing application (text ads only). Given a background in 
marketing and advertising and my ongoing study of functional programming (Scala), 
this project aims to provide a relevant and compelling context.

## Initial Focus
I will initially focus on building out the API in http4s, as per competencies in Cats and
Cats Effect and a focus on a strong functional approach. To begin, a simple in-script 'database' 
will be used to test functionality before transitioing to a proper persistence layer. 

Key API features will include but are not limited to: 
- Tagless final design pattern to ensure a high level of flexibility via parametric polymorphism. 
- Routing to enable a variety of HTTP requests from user, including test submission.
- Versioning to ensure continuity of service after updates.
- Finite state machine implementation for complex state management.
- Unit testing and integration testing, as per test-driven approaches.
- Persistence layer: NoSQL (Cassandara) to be considered initially for horizontal scalability, fault tolerance 
and high read/write tolerance (envisioned application would have to ingest performance metrics in real time). Possibility 
of transitioning to hybrid SQL/NoSQL strategy if more compelx, cross-table queries necessary.
- Security measures: Authentication and authorisation (JWT), rate limiting and runtime validation for 
  targeted error handling and security. 
- General consideration for concurrent workflows, scalability and fault tolerance.

