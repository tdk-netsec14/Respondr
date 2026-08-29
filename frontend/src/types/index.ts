export type MemberRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export type IncidentSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export type IncidentStatus =
  | 'OPEN'
  | 'ACKNOWLEDGED'
  | 'INVESTIGATING'
  | 'RESOLVED'
  | 'CANCELLED'
  | 'REOPENED'
  | 'CLOSED';

export interface User {
  id: string;
  email: string;
  name: string;
  status: string;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
}

export interface Team {
  id: string;
  orgId: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceEntity {
  id: string;
  orgId: string;
  teamId?: string;
  name: string;
  key: string;
  description?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Incident {
  id: string;
  orgId: string;
  serviceId?: string;
  teamId?: string;
  title: string;
  description?: string;
  severity: IncidentSeverity;
  status: IncidentStatus;
  source?: string;
  assigneeId?: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}

export interface IncidentEvent {
  id: string;
  incidentId: string;
  eventType: string;
  actorId?: string;
  metadata?: string;
  createdAt: string;
}

export interface Comment {
  id: string;
  incidentId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface OnCallSchedule {
  id: string;
  orgId: string;
  teamId: string;
  teamName: string;
  name: string;
  timezone: string;
  rotationRules: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleOverride {
  id: string;
  scheduleId: string;
  userId: string;
  userName: string;
  startAt: string;
  endAt: string;
  reason?: string;
  createdAt: string;
}

export interface ResponderResponse {
  userId: string;
  userName: string;
  userEmail: string;
  scheduleId: string;
  scheduleName: string;
  isOverride: boolean;
  timestamp: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
  organization: Organization;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  traceId: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
